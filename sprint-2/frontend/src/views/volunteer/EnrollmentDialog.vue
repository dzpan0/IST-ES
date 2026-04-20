<template>
  <v-dialog v-model="dialog" persistent width="800">
    <v-card>
      <v-card-title>
        <span class="headline">
          {{
            editEnrollment && editEnrollment.id === null
              ? 'New Application'
              : 'Edit Application'
          }}
        </span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" lazy-validation>
          <v-row>
            <v-col cols="12">
              <v-textarea
                label="*Motivation"
                :rules="[(v) => !!v || 'Motivation is required']"
                required
                v-model="editEnrollment.motivation"
                data-cy="motivationInput"
                auto-grow
                rows="1"
              ></v-textarea>
            </v-col>
            <v-col cols="12">
              <v-select
                label="*Shifts"
                :disabled="editEnrollment.id != null"
                :rules="[
                  (v) => (Array.isArray(v) && v.length > 0) || 'At least one shift is required'
                ]"
                required
                v-model="editEnrollment.shiftIds"
                :items="availableShifts"
                item-text="label"
                item-value="id"
                multiple
                chips
                data-cy="shiftsMultiSelect"
              ></v-select>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="blue-darken-1"
          variant="text"
          @click="$emit('close-enrollment-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          v-if="canSave"
          color="blue-darken-1"
          variant="text"
          @click="updateEnrollment"
          data-cy="saveEnrollment"
        >
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script lang="ts">
import { Vue, Component, Prop, Model } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import { ISOtoString } from '@/services/ConvertDateService';
import Enrollment from '@/models/enrollment/Enrollment';
import Shift from '@/models/shift/Shift';

@Component({
  methods: { ISOtoString },
})
export default class EnrollmentDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Enrollment, required: true }) readonly enrollment!: Enrollment;
  @Prop({ type: Array, required: true }) readonly shifts!: Shift[];

  editEnrollment: Enrollment = new Enrollment();

  async created() {
    this.editEnrollment = new Enrollment(this.enrollment);
  }

  get availableShifts() {
    return this.shifts.map((shift: Shift) => ({
      id: shift.id,
      label: `${shift.location} (${shift.formattedStartTime} - ${shift.formattedEndTime})`,
    }));
  }

  get hasOverlappingShifts(): boolean {
    const selected = this.shifts.filter((shift) => 
      this.editEnrollment.shiftIds?.includes(shift.id!)
    );
    return selected.some((a, i) =>
      selected.some((b, j) => i !== j &&
        a.startTime < b.endTime && b.startTime < a.endTime
      )
    );
  }

  get canSave(): boolean {
    return (
      !!this.editEnrollment.motivation &&
      this.editEnrollment.motivation.length >= 10 &&
      this.editEnrollment.shiftIds &&
      this.editEnrollment.shiftIds.length > 0 &&
      !this.hasOverlappingShifts
    );
  }

  async updateEnrollment() {
    //editar
    if (
      this.editEnrollment.id !== null &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      try {
        const result = await RemoteServices.editEnrollment(
          this.editEnrollment.id,
          this.editEnrollment,
        );
        this.$emit('update-enrollment', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
    //criar
    else if (
      this.editEnrollment.activityId !== null &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      try {
        const result = await RemoteServices.createEnrollment(
          this.editEnrollment,
        );
        this.$emit('save-enrollment', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }
}
</script>

<style scoped lang="scss"></style>
