<template>
  <v-dialog v-model="dialog" persistent width="800">
    <v-card>
      <v-card-title>
        <span class="headline">
          {{
            editParticipation && editParticipation.id === null
              ? 'Create Participation'
              : 'Your Rating'
          }}
        </span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" lazy-validation>
          <v-row>
            <v-col cols="12" v-if="isCreating">
              <v-select
                label="*Shift"
                :rules="[(v) => !!v || 'A shift is required']"
                required
                v-model="editParticipation.shiftId"
                :items="availableShifts"
                item-text="label"
                item-value="id"
                data-cy="participationShiftSelect"
              ></v-select>
            </v-col>
            <v-col v-else cols="12">
              <div>
                <strong>Shift:</strong>
                <div class="mt-2">
                  <v-chip>
                    {{ getShiftLabel(editParticipation.shiftId) }}
                  </v-chip>
                </div>
              </div>
            </v-col>
            <v-col cols="12" class="d-flex align-center">
              <v-text-field
                label="Rating"
                :rules="[(v) => isNumberValid(v) || 'Rating between 1 and 5']"
                v-model="editParticipation.memberRating"
                data-cy="participantsNumberInput"
              ></v-text-field>
            </v-col>
            <v-col cols="12">
              <v-textarea
                label="Review"
                v-model="editParticipation.memberReview"
                :rules="[(v) => !!v || 'Review is required']"
                data-cy="participantsReviewInput"
                auto-grow
                rows="1"
              ></v-textarea>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="primary"
          dark
          variant="text"
          @click="$emit('close-participation-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          v-if="canSave"
          color="primary"
          dark
          variant="text"
          @click="createUpdateParticipation"
          data-cy="createParticipation"
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
import Participation from '@/models/participation/Participation';
import Shift from '@/models/shift/Shift';

@Component({
  methods: { ISOtoString },
})
export default class ParticipationSelectionDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Participation, required: true })
  readonly participation!: Participation;
  @Prop({ type: Array, required: true }) readonly enrolledShifts!: Shift[];
  @Prop({ type: Number, required: true }) readonly enrollmentId!: number;

  participations: Participation[] = [];
  editParticipation: Participation = new Participation();

  async created() {
    this.editParticipation = new Participation(this.participation);
    this.participations = await RemoteServices.getActivityParticipations(
      this.editParticipation.activityId,
    );
  }

  get isCreating(): boolean {
    return this.editParticipation.id === null;
  }

  get availableShifts() {
    return this.enrolledShifts.map((shift: Shift) => ({
      id: shift.id,
      label: `${shift.location} (${shift.formattedStartTime} - ${shift.formattedEndTime})`,
    }));
  }

  getShiftLabel(shiftId: number | null): string {
    if (shiftId === null) return 'Unknown Shift';
    const shift = this.enrolledShifts.find((s) => s.id === shiftId);
    if (!shift) return 'Unknown Shift';
    return `${shift.location} (${shift.formattedStartTime} - ${shift.formattedEndTime})`;
  }

  getParticipantsCountForShift(shiftId: number | null): number {
    if (shiftId === null) return 0;
    return this.participations.filter((p) => p.shiftId === shiftId).length;
  }

  get canSave(): boolean {
    if (this.isCreating) {
      const shift = this.enrolledShifts.find((s) => s.id === this.editParticipation.shiftId);
      if (!shift) {
        return false;
      }

      const participantsCount = this.getParticipantsCountForShift(this.editParticipation.shiftId);
      const shiftValid = this.editParticipation.shiftId !== null &&
        participantsCount < shift.participantsLimit;
      return this.isReviewValid && this.isRatingValid && shiftValid;
    } else {
      return this.isReviewValid && this.isRatingValid;
    }
  }

  get isReviewValid(): boolean {
    return (
      !this.editParticipation.memberReview ||
      (this.editParticipation.memberReview.length >= 10 &&
        this.editParticipation.memberReview.length < 100)
    );
  }

  get isRatingValid(): boolean {
    return (
      !this.editParticipation.memberRating ||
      (this.editParticipation.memberRating >= 1 &&
        this.editParticipation.memberRating <= 5)
    );
  }

  isNumberValid(value: any) {
    if (value === null || value === undefined || value === '') return true;
    if (!/^\d+$/.test(value)) return false;
    const parsedValue = parseInt(value);
    return parsedValue >= 1 && parsedValue <= 5;
  }

  async createUpdateParticipation() {
    if ((this.$refs.form as Vue & { validate: () => boolean }).validate()) {
      try {
        const result =
          this.editParticipation.id !== null
            ? await RemoteServices.updateParticipationMember(
                this.editParticipation.id,
                this.editParticipation,
              )
            : await RemoteServices.createParticipation(
                this.editParticipation.shiftId!,
                this.enrollmentId,
                this.editParticipation,
              );
        this.$emit('save-participation', result);
        this.$emit('close-participation-dialog');
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }
}
</script>

<style scoped lang="scss"></style>
