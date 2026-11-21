// Production builds are served behind the ingress, which routes /api to the
// gateway on the same origin, so API calls use relative URLs.
export const environment = {
  apiUrl: ''
};
