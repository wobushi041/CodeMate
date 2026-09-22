const CONTACTED_USER_IDS_KEY = 'codemate:contacted-user-ids';

const readContactedUserIds = (): number[] => {
  if (typeof window === 'undefined') return [];

  try {
    const parsed = JSON.parse(window.localStorage.getItem(CONTACTED_USER_IDS_KEY) || '[]');
    return Array.isArray(parsed) ? parsed.map(Number).filter(Number.isFinite) : [];
  } catch {
    return [];
  }
};

export const getContactedUserIds = (): number[] => readContactedUserIds();

export const markUserContacted = (userId: number | string | undefined | null): void => {
  const normalizedUserId = Number(userId);
  if (!Number.isFinite(normalizedUserId) || typeof window === 'undefined') return;

  const contactedUserIds = readContactedUserIds();
  if (!contactedUserIds.includes(normalizedUserId)) {
    window.localStorage.setItem(
      CONTACTED_USER_IDS_KEY,
      JSON.stringify([...contactedUserIds, normalizedUserId]),
    );
  }
};
