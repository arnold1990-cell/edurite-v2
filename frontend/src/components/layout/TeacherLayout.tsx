import { Outlet } from 'react-router-dom';

export const TeacherLayout = () => (
  <div className="min-h-screen overflow-x-hidden bg-slate-100 px-3 py-3 md:px-4 md:py-4">
    <div className="mx-auto w-full max-w-[1900px]">
      <Outlet />
    </div>
  </div>
);

