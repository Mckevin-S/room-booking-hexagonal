'use client';

import { useEffect, useState } from 'react';
import { api } from './api';
import { useSession } from './session';
import type { Site } from './types';

/** Sites de l'entreprise, et site sélectionné (celui du collaborateur par défaut). */
export function useSites() {
  const { user } = useSession();
  const [sites, setSites] = useState<Site[]>([]);
  const [siteId, setSiteId] = useState<string>('');

  useEffect(() => {
    api.sites().then((all) => {
      setSites(all);
      setSiteId((current) => current || (all.some((s) => s.id === user?.homeSiteId) ? user!.homeSiteId : all[0]?.id) || '');
    }).catch(() => undefined);
  }, [user]);

  return { sites, siteId, setSiteId, site: sites.find((s) => s.id === siteId) };
}
