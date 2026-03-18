export type ProfileFlag = {
  code: string;
  severity: string;
  active: boolean;
  title: string;
  message: string;
};

export type ProfileAnalysis = {
  totalConnections: number;
  strategicConnections: number;
  strategicConnectionsRatio: number;
  totalProjects: number;
  totalSkills: number;
  flags: ProfileFlag[];
  summary: string;
};
