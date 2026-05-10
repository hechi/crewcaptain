import { ActionItemOwnerType } from './action-item';

export interface DashboardActionItem {
  id: string;
  personId: string;
  personName: string;
  title: string;
  dueDate: string;
  ownerType: ActionItemOwnerType;
}

export type CadenceType = 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'CUSTOM';

export interface StaleOneOnOneReminder {
  personId: string;
  personName: string;
  cadenceType: CadenceType;
  customIntervalDays: number | null;
  lastMeetingDate: string | null;
  daysSinceLastMeeting: number;
  expectedIntervalDays: number;
}

export interface UpcomingAnniversary {
  personId: string;
  personName: string;
  startDate: string;
  anniversaryDate: string;
  yearsCompleted: number;
  daysUntil: number;
}

export interface DashboardResponse {
  overdueActionItems: DashboardActionItem[];
  dueSoonActionItems: DashboardActionItem[];
  staleOneOnOnes: StaleOneOnOneReminder[];
  upcomingAnniversaries: UpcomingAnniversary[];
}
