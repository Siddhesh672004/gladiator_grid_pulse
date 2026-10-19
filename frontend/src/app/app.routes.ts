import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { ADMIN_ROLES, ROLES } from './core/constants';

// url -> lazy module. admin / user pages: the AuthGuard runs first. everything else is in the site module
export const routes: Routes = [
  {
    path: 'admin',
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    data: { roles: ADMIN_ROLES },
    loadChildren: () => import('./admin/admin.module').then((m) => m.AdminModule)
  },
  { 
    path: 'user',
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    data: { roles: [ROLES.USER] },
    loadChildren: () => import('./user/user.module').then((m) => m.UserModule)
  },
  {
    path: '',
    loadChildren: () => import('./site/site.module').then((m) => m.SiteModule)
  }
];
