import { UserPlus } from 'lucide-react';
import { Button } from '@/components/ui/Button';

const quickActions = [
  'Add Teacher',
  'Add Learner',
  'Create Class',
  'Create Subject',
  'Assign Subject',
  'Send Notification',
  'View Reports',
];

export const AdminQuickActions = ({ setActiveSection }: { setActiveSection: (section: string) => void }) => (
  <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
    <h2 className="text-sm font-semibold text-slate-900">Quick Actions</h2>
    <div className="mt-3 grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
      {quickActions.map((label) => (
        <Button key={label} type="button" className="justify-start rounded-xl bg-slate-100 text-slate-800 hover:bg-slate-200" onClick={() => setActiveSection(label.includes('Subject') ? 'Subjects' : label.includes('Teacher') ? 'Teachers' : label.includes('Learner') ? 'Learners' : label.includes('Class') ? 'Classes' : label.includes('Report') ? 'Reports' : 'Notifications')}>
          <UserPlus className="mr-2 h-4 w-4" />
          {label}
        </Button>
      ))}
    </div>
  </div>
);
