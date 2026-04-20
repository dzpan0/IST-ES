<template>
  <v-card class="table">
    <div class="text-h3">{{ activity.name }}</div>
    <v-data-table
      :headers="headers"
      :items="shifts"
      :search="search"
      disable-pagination
      :hide-default-footer="true"
      :mobile-breakpoint="0"
      data-cy="activityShiftsTable"
    >
      <template v-slot:top>
        <v-card-title>
          <v-text-field
            v-model="search"
            append-icon="search"
            label="Search"
            class="mx-2"
          />
          <v-spacer />
          <v-btn
            color="primary"
            dark
            @click="getActivities"
            data-cy="getActivities"
          >Activities</v-btn
          >
          <v-btn
            :disabled="!canCreateShift"
            color="primary"
            @click="newShift"
            data-cy="newShift"
          >New Shift</v-btn
          >
        </v-card-title>
      </template>
    </v-data-table>
    <shift-dialog
      v-if="currentShift && createShiftDialog"
      v-model="createShiftDialog"
      :shift="currentShift"
      v-on:save-shift="onSaveShift"
      v-on:close-shift-dialog="onCloseShiftDialog"
    />
  </v-card>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import Activity from '@/models/activity/Activity';
import Shift from '@/models/shift/Shift';
import ShiftDialog from '@/views/member/ShiftDialog.vue';

@Component({
  components: {
    'shift-dialog': ShiftDialog,
  },
})
export default class InstitutionActivityShiftsView extends Vue {
  activity!: Activity;
  shifts: Shift[] = [];
  search: string = '';

  currentShift: Shift | null = null;
  createShiftDialog: boolean = false;

  headers: object = [
    {
      text: 'Participant Limit',
      value: 'participantsLimit',
      align: 'left',
      width: '20%',
    },
    {
      text: 'Start Time',
      value: 'formattedStartTime',
      align: 'left',
      width: '20%',
    },
    {
      text: 'End Time',
      value: 'formattedEndTime',
      align: 'left',
      width: '20%',
    },
    {
      text: 'Location',
      value: 'location',
      align: 'left',
      width: '20%',
    },
  ];

  async created() {
    this.activity = this.$store.getters.getActivity;
    if (this.activity !== null && this.activity.id !== null) {
      await this.$store.dispatch('loading');
      try {
        this.shifts = await RemoteServices.getActivityShifts(this.activity.id);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
      await this.$store.dispatch('clearLoading');
    }
  }

  async getActivities() {
    await this.$store.dispatch('setActivity', null);
    this.$router.push({ name: 'institution-activities' }).catch(() => {});
  }

  get canCreateShift(): boolean {
    return this.activity?.state === 'APPROVED';
  }

  newShift() {
    this.currentShift = new Shift();
    this.currentShift.activityId = this.activity.id;
    this.createShiftDialog = true;
  }

  onSaveShift(shift: Shift) {
    this.shifts.push(shift);
    this.activity.shifts.push(shift);
    this.currentShift = null;
    this.createShiftDialog = false;
  }

  onCloseShiftDialog() {
    this.currentShift = null;
    this.createShiftDialog = false;
  }
}
</script>

<style lang="scss" scoped>
.date-fields-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.date-fields-row {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}
</style>
