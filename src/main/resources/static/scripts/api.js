/* global SwaggerUIBundle, SwaggerUIStandalonePreset */

window.onload = function() {
  window.ui = SwaggerUIBundle({
    url: "api.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });
};