// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

// Same-origin URLs as in production: the `ng serve` dev server proxies
// /v1 (data accessor, :8083) and /livedata (frontend Ktor, :8085) per
// proxy.conf.json, mirroring the Traefik ingress routing.
export const environment = {
  production: false,
  serverHost: location.host,
  serverUrl: `${location.protocol}//${location.host}`,
  webSocketProtocol: `${location.protocol === 'https:' ? 'wss' : 'ws'}`
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
