export const environment = {
  production: true,
  serverHost: location.host,
  serverUrl: `${location.protocol}//${location.host}`,
  webSocketProtocol: `${location.protocol === 'https:' ? 'wss' : 'ws'}`
};
