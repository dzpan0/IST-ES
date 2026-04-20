<template>
  <v-dialog v-model="dialog" persistent width="1300">
    <v-card>
      <v-card-title>
        <span class="headline">New Shift</span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" lazy-validation>
          <v-row>
            <v-col cols="12">
              <v-text-field
                label="*Location"
                :rules="[
                  (v) => !!v || 'Location is required',
                  (v) => v.length >= 20 || 'Location must be at least 20 characters',
                  (v) => v.length <= 200 || 'Location must be at most 200 characters'
                ]"
                required
                v-model="editShift.location"
                data-cy="locationInput"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <v-text-field
                label="*Number of Participants"
                :rules="[(v) => !!v || 'Number of participants is required']"
                required
                v-model="editShift.participantsLimit"
                data-cy="participantsNumberInput"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <VueCtkDateTimePicker
                id="startingDateInput"
                v-model="editShift.startTime"
                format="YYYY-MM-DDTHH:mm:ssZ"
                label="*Starting Date"
              ></VueCtkDateTimePicker>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <VueCtkDateTimePicker
                id="endingDateInput"
                v-model="editShift.endTime"
                format="YYYY-MM-DDTHH:mm:ssZ"
                label="*Ending Date"
              ></VueCtkDateTimePicker>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="blue-darken-1"
          variant="text"
          @click="$emit('close-shift-dialog')"
          data-cy="cancelShift"
        >
          Close
        </v-btn>
        <v-btn
          color="blue-darken-1"
          variant="text"
          :disabled="!isLocationValid"
          @click="createShift"
          data-cy="saveShift"
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
import Shift from '@/models/shift/Shift';
import VueCtkDateTimePicker from 'vue-ctk-date-time-picker';
import 'vue-ctk-date-time-picker/dist/vue-ctk-date-time-picker.css';

Vue.component('VueCtkDateTimePicker', VueCtkDateTimePicker);
@Component({
  methods: { ISOtoString },
})
export default class ShiftDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Shift, required: true }) readonly shift!: Shift;

  editShift: Shift = new Shift();

  async created() {
    this.editShift = new Shift(this.shift);
  }

  async createShift() {
    if ((this.$refs.form as Vue & { validate: () => boolean }).validate()) {
      try {
        const result = await RemoteServices.createShift(
          this.editShift.activityId!,
          this.editShift,
        );
        this.$emit('save-shift', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }

  get isLocationValid(): boolean {
    const location = this.editShift.location ?? '';
    return location.length >= 20 && location.length <= 200;
  }
}

</script>

<style scoped lang="scss"></style>
