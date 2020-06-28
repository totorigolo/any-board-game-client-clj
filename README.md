<div align="center">

  <h1><code>cards-client-clj</code></h1>

  <strong>A superb cards game web app to play with friends.</strong>
  <br>
  <i>At least that's what we are trying to achieve.</i>

  <p>
    <a href="https://travis-ci.org/totorigolo/cards-client-rs">
      <img src="https://api.travis-ci.com/totorigolo/cards-client-rs.svg?branch=master" alt="Build Status" />
    </a>
  </p>

  <h3>
    <a href="https://cards.busy.ovh/">Play online</a>
    <span> | </span>
    <a href="#">Dev corner</a>
  </h3>

  <sub>Built in Clojure(Script), using [re-frame](https://day8.github.io/re-frame/).</sub>
</div>

## About

TBD

## 🚴 Usage

If you just want to play just head to our [web site](https://cards.busy.ovh/).
If instead you enjoy playing with code, or you want to give us a hand, keep
reading.

### 🐑 Prerequisites

1. Install [Clojure](https://clojure.org/), following [those instructions](https://purelyfunctional.tv/guide/how-to-install-clojure/).
2. Install [Node.js](https://nodejs.org/) (for `npm`).
3. Install [Caddy](https://caddyserver.com/docs/download), for reverse proxy.
   * We recommend using the version from the official distribution.
   * Be careful to install Caddy v2.

### 🛠️ How to run it

```bash
# Needed only the first time
lein deps

# To build and start a web server, with hot-reload enabled.
# Afterwards, go to: http://localhost:8280/.
# However, this won't serve the API. Read below for how-to.
lein dev

# To build the Less files to CSS, use one of:
lein less auto # for live reload
lein less once
```

The instructions above are for the frontend only. If you want the full website,
i.e. with the API, you need to run the game server and serve it at `/api`.

To simplify this setup, there is a `Caddyfile` in this repository that comes
pre-configured. Just make sure to respect the ports explained in the file.

```bash
# To start the reverse proxy
caddy run --watch

# Or to start as a daemon
caddy start --watch
caddy stop
```

Then, go to http://localhost:42800/, where the API is now available at `/api`.

For more details, see [README.re-frame.md](./README.re-frame.md).
