import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

// base "./" so the bundle works at a project-pages subpath or a custom-domain root (D12).
// build.target es2022 (D12). The Scala.js plugin links the `app` module and exposes its
// output to the `scalajs:main.js` virtual import (see main.ts).
export default defineConfig({
  base: "./",
  build: { target: "es2022" },
  plugins: [
    scalaJSPlugin({
      // sbt project that produces the application bundle
      projectID: "app"
    })
  ]
});
