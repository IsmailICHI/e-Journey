import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'home',
    },
    {
        path: 'home',
        component: HomeComponent,
    },
    {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login.component').then((page) => page.LoginComponent),
},
{
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register/register.component').then((page) => page.RegisterComponent),
},
    {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () => import('./pages/profile/profile.component').then((page) => page.ProfileComponent),
    },
    {
        path: 'dashboard',
        pathMatch: 'full',
        redirectTo: 'home',
    },
    {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/admin/admin.component').then((page) => page.AdminComponent),
    },
    {
        path: 'error/:status',
        loadComponent: () => import('./pages/error/error.component').then((page) => page.ErrorComponent),
    },
    {
        path: 'forbidden',
        pathMatch: 'full',
        redirectTo: 'error/403',
    },
    {
        path: '**',
        redirectTo: 'error/404',
    },
];
