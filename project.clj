(defproject cards-client-clj "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs "2.10.17"]
                 [reagent "0.10.0"]
                 [re-frame "1.0.0"]
                 [clj-commons/secretary "1.2.4"]
                 [re-pressed "0.3.1"]
                 [breaking-point "0.1.2"]
                 [day8.re-frame/http-fx "0.1.6"]]
  :plugins [[lein-shadow "0.2.0"]
            [lein-scss "0.3.0"]
            [lein-shell "0.5.0"]
            [lein-cljfmt "0.6.7"]]
  :min-lein-version "2.9.0"
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["src/clj" "src/cljs" "test/clj" "test/cljs"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"
                                    "test/js"]
  :scss {:builds
         {:develop    {:source-dir "resources/sass"
                       :dest-dir   "resources/public/css"
                       :executable "sassc"
                       :args       ["-m" "-I" "scss/" "-t" "nested"]}
          :production {:source-dir "resources/sass"
                       :dest-dir   "resources/public/css"
                       :executable "sassc"
                       :args       ["-I" "scss/" "-t" "compressed"]}}}
  :sass {:src "resources/sass"
         :output-directory "resources/public/css"
         :command :scss}
  :shell {:commands {"open" {:windows ["cmd" "/c" "start"]
                             :macosx  "open"
                             :linux   "xdg-open"}}}
  :shadow-cljs {:nrepl {:port 8777}
                :builds {:app {:target :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :compiler-options {:infer-externs :auto}
                               :modules {:app {:init-fn cards-client-clj.core/init
                                               :preloads [devtools.preload
                                                          re-frisk.preload]}}
                               :dev {:compiler-options {:closure-defines {}}}
                               :release {:build-options
                                         {:ns-aliases
                                          {}}}
                               :devtools {:http-root "resources/public"
                                          :http-port 42803}}
                         :browser-test {:target :browser-test
                                        :ns-regexp "-test$"
                                        :runner-ns shadow.test.browser
                                        :test-dir "target/browser-test"
                                        :devtools {:http-root "target/browser-test"
                                                   :http-port 8290}}
                         :karma-test {:target :karma
                                      :ns-regexp "-test$"
                                      :output-to "target/karma-test.js"}}}
  :aliases {"dev"          ["with-profile" "dev" "do"
                            ["shadow" "watch" "app"]]
            "prod"         ["with-profile" "prod" "do"
                            ["shadow" "release" "app"]]
            "build-report" ["with-profile" "prod" "do"
                            ["shadow" "run" "shadow.cljs.build-report" "app" "target/build-report.html"]
                            ["shell" "open" "target/build-report.html"]]
            "karma"        ["with-profile" "prod" "do"
                            ["shadow" "compile" "karma-test"]
                            ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots" "--browsers" "ChromiumHeadless"]]}
  :profiles  {:dev {:dependencies [[binaryage/devtools "1.0.2"]
                                   [re-frisk "1.3.4"]]
                    :source-paths ["dev"]}
              :prod {:dependencies []}})
