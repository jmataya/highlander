/* @flow */

import React, { Component, Element } from 'react';
import { Route, IndexRoute } from 'react-router';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import UsersListPage from 'components/users/user-list';

import Users from 'components/users/users';
import User from 'components/users/user';
import UserForm from 'components/users/user-form';

import PluginsList from 'components/plugins/plugins-list';
import Plugin from 'components/plugins/plugin';

import type { JWT } from 'lib/claims';

const getRoutes = (jwt: JWT) => {
  const router = new FoxRouter(jwt);

  const userRoutes =
    router.read('user-base', { path: 'users', frn: frn.settings.user }, [
      router.read('users-list-page', { component: UsersListPage }, [
        router.read('users', { component: Users, isIndex: true }),
      ]),
      router.read('user', { path: ':userId', component: User }, [
        router.read('user-form', { component: UserForm, isIndex: true }),
        router.read('user-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.user,
        }),
      ]),
    ]);

  const pluginRoutes =
    router.read('plugins-base', { path: 'plugins', frn: frn.settings.plugin }, [
      router.read('plugins', { component: PluginsList, isIndex: true }),
      router.read('name', { path: ':name', component: Plugin }),
    ]);

  return (
    <div>
      {userRoutes}
      {pluginRoutes}
    </div>
  );
}

export default getRoutes;
