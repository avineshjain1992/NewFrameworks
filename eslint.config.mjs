import js from "./playwright-javascript/node_modules/@eslint/js/src/index.js";
export default [
  {
    ignores: [
      "**/target/**",
      "**/reports/**",
      "**/artifacts/**",
      "**/node_modules/**",
      ".local/**",
      "**/mvnw*",
    ],
  },
  {
    files: ["demo/**/*.js", "scripts/**/*.mjs"],
    ...js.configs.recommended,
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        process: "readonly",
        console: "readonly",
        URL: "readonly",
        setTimeout: "readonly",
        clearTimeout: "readonly",
        fetch: "readonly",
        Buffer: "readonly",
      },
    },
  },
];
