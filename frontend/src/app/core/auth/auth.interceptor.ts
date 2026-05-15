import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const user = auth.currentUser();

  if (user?.email) {
    return next(req.clone({ setHeaders: { 'X-Mock-User-Id': user.email } }));
  }

  return next(req);
};
