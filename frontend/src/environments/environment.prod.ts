// used by "ng build" (angular.json fileReplacements): the site and the api share one address in production
// (caddy serves the site and passes /api to the gateway), so every call is relative
export const environment = {
  production: true,
  apiUrl: ''
};
