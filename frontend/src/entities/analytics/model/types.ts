export type CountItem = {
  value: string;
  total: number;
};

export type StatsResponse = {
  totalConnections: number;
  topCompanies: CountItem[];
  topPositions: CountItem[];
};

export type StrategicContact = {
  id: number;
  firstName: string;
  lastName: string;
  emailAddress: string | null;
  company: string | null;
  position: string | null;
  connectedOn: string | null;
};
