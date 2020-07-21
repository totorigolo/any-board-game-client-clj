# Tests

## Test organization

Tests can be located at different locations, depending on their type (eg. unit,
integration) or clojure "dialect" (`.clj`, `.cljc` or `.cljs`).

### Inline (unit) tests

**`.clj` and `.cljc` files only**, as `lein test` doesn't find them in `.cljs`.

Unit tests can be written directly inside the function, which is convenient for
small-to-medium tests as they are directly next to what they test, and can serve
as documentation.

```Clojure
(defn adds-some-fun
  "Adds some fun."
  {:test (fn []
           (is (= {:boring "string"}
                  {:boring "string" :fun true}))
           (testing "something <- this also works"
            (is true)))}
  [map]
  (is (map? input))
  (assoc map :fun true))
```

To run those tests, simple use `lein test`.

### Unit tests

**In `.clj` and `.cljc` files only**, unit tests can be located directly inside
files where the functions are defined, inside the `src/` directory.

Otherwise, for all Clojure "dialects", they can be located inside `test/`. In
that case, the files must have the same name than the tested file, with `_test`
appended before the extension, and the file path must reflect the one of the
tested file.

To run those tests, use `lein test` for non-`.cljs` files. For ClojureScript, as
they must run inside a JS environment, they are run using Karma: `lein karma`.

### Integration test

TBD

### Devcards

TBD

- https://github.com/bhauman/devcards
- https://github.com/oliyh/kamera

### Webdriver tests

TBD

- https://github.com/igrishaev/etaoin
