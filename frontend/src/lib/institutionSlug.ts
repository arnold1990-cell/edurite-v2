export const createInstitutionSlug = (value?: string | null) => {
  if (!value) return '';
  return value
    .toLowerCase()
    .trim()
    .replace(/&/g, '-')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
};
