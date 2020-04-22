(ns todo-backend.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.cors :refer [wrap-cors]]
            [reitit.ring :as ring]))

(defn ok [body]
  {:status 200
   :body body})

(def app-routes
  (ring/ring-handler
   (ring/router
    [["/" {:get {:handler (fn [req] (ok "OK GET"))}
           :post {:handler (fn [req] (ok "OK POST"))}}]
     ["/todos" {:post {:handler (fn [req] (ok "OK POST"))}}]])))

(def handler
  (wrap-cors app-routes :access-control-allow-origin [#".*"]
                        :access-control-allow-methods [:get :put :post :delete]))

(defn -main [port]
  (jetty/run-jetty #'handler {:port (Integer. port)
                              :join? false}))

(comment
  (def server (jetty/run-jetty #'handler {:port 3000
                                          :join? false})))
