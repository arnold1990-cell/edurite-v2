import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

const loginSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required').min(8, 'Password must be at least 8 characters')
});

const passwordRuleMessage = 'Password must be at least 8 characters and include a number';

const studentRegisterSchema = z.object({
  firstName: z.string().trim().min(1, 'First name is required'),
  lastName: z.string().trim().min(1, 'Last name is required'),
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().refine((value) => value.length >= 8 && /\d/.test(value), passwordRuleMessage),
  interests: z.string().trim().optional(),
  location: z.string().trim().optional(),
  phone: z.string().trim().optional(),
  dateOfBirth: z.string().trim().optional(),
  gender: z.string().trim().optional(),
  qualificationLevel: z.string().trim().optional(),
  companyName: z.string().trim().optional()
});

const companyRegisterSchema = z.object({
  firstName: z.string().trim().optional(),
  lastName: z.string().trim().optional(),
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().refine((value) => value.length >= 8 && /\d/.test(value), passwordRuleMessage),
  interests: z.string().trim().optional(),
  location: z.string().trim().optional(),
  phone: z.string().trim().optional(),
  dateOfBirth: z.string().trim().optional(),
  gender: z.string().trim().optional(),
  qualificationLevel: z.string().trim().optional(),
  companyName: z.string().trim().optional()
});

const inputErrorClass = 'border-red-500 focus:ring-red-500';

type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof studentRegisterSchema>;

export const LoginForm = ({ onSubmit }: { onSubmit: (data: LoginFormValues) => Promise<void> }) => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  return (
    <form className="space-y-5" onSubmit={handleSubmit(onSubmit)} noValidate>
      <label className="block text-sm text-slate-800">
        Email
        <Input className="mt-2 rounded-xl border-slate-300 bg-slate-100 px-4 py-3 text-sm" {...register('email')} type="email" />
      </label>
      {errors.email && <p className="text-xs text-red-600">{errors.email.message}</p>}
      <label className="block text-sm text-slate-800">
        Password
        <Input className="mt-2 rounded-xl border-slate-300 bg-slate-100 px-4 py-3 text-sm" {...register('password')} type="password" />
      </label>
      {errors.password && <p className="text-xs text-red-600">{errors.password.message}</p>}
      <Button disabled={isSubmitting} type="submit" className="rounded-xl px-6 py-3 text-sm">Sign In</Button>
    </form>
  );
};

export const RegisterForm = ({ type, onSubmit }: { type: 'student' | 'company'; onSubmit: (data: RegisterFormValues) => Promise<void> }) => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(type === 'student' ? studentRegisterSchema : companyRegisterSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      interests: '',
      location: '',
      phone: '',
      dateOfBirth: '',
      gender: '',
      qualificationLevel: '',
      companyName: ''
    }
  });

  return (
    <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
      {type === 'student' && (
        <>
          <label className="block text-sm">
            First Name
            <Input className={errors.firstName ? inputErrorClass : ''} autoComplete="given-name" {...register('firstName')} />
          </label>
          {errors.firstName && <p className="text-xs text-red-600">{errors.firstName.message}</p>}

          <label className="block text-sm">
            Last Name
            <Input className={errors.lastName ? inputErrorClass : ''} autoComplete="family-name" {...register('lastName')} />
          </label>
          {errors.lastName && <p className="text-xs text-red-600">{errors.lastName.message}</p>}

          <label className="block text-sm">
            Interests (optional)
            <Input className={errors.interests ? inputErrorClass : ''} {...register('interests')} placeholder="e.g. Engineering, Coding" />
          </label>

          <label className="block text-sm">
            Location (optional)
            <Input className={errors.location ? inputErrorClass : ''} {...register('location')} />
          </label>

          <label className="block text-sm">
            Phone (optional)
            <Input className={errors.phone ? inputErrorClass : ''} autoComplete="tel" {...register('phone')} />
          </label>

          <label className="block text-sm">
            Date of Birth (optional)
            <Input className={errors.dateOfBirth ? inputErrorClass : ''} type="date" {...register('dateOfBirth')} />
          </label>

          <label className="block text-sm">
            Gender (optional)
            <Input className={errors.gender ? inputErrorClass : ''} {...register('gender')} />
          </label>

          <label className="block text-sm">
            Qualification Level (optional)
            <Input className={errors.qualificationLevel ? inputErrorClass : ''} {...register('qualificationLevel')} placeholder="e.g. High School" />
          </label>
        </>
      )}

      <label className="block text-sm">
        Email
        <Input className={errors.email ? inputErrorClass : ''} autoComplete="email" {...register('email')} type="email" />
      </label>
      {errors.email && <p className="text-xs text-red-600">{errors.email.message}</p>}

      <label className="block text-sm">
        Password
        <Input className={errors.password ? inputErrorClass : ''} autoComplete="new-password" {...register('password')} type="password" />
      </label>
      <p className="text-xs text-slate-500">{passwordRuleMessage}.</p>
      {errors.password && <p className="text-xs text-red-600">{errors.password.message}</p>}

      {type === 'company' && <label className="block text-sm">Company Name<Input {...register('companyName')} /></label>}
      <Button disabled={isSubmitting} type="submit">Create Account</Button>
    </form>
  );
};
