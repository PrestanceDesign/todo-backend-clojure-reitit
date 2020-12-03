(ns todo-backend.core
  (:require [juxt.clip.core :as clip]
            [muuntaja.core :as m]
            [reitit.coercion.schema :as rcs]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as rrmm]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.cors :refer [wrap-cors]]
            [schema.core :as s]
            [todo-backend.store :as store]
            [todo-backend.system :refer [system-config]]))

(defn ok [body]
  {:status 200
   :body body})

(defn append-todo-url [todo request]
  (let [host (-> request :headers (get "host" "localhost"))
        scheme (name (:scheme request))
        id (:id todo)]
    (merge todo {:url (str scheme "://" host "/todos/" id)})))

(defn app-routes [db]
  (ring/ring-handler
   (ring/router
    [["/swagger.json" {:get
                       {:no-doc true
                        :swagger {:basePath "/"
                                  :info {:title "Todo-Backend API"
                                         :description "This is a implementation of the Todo-Backend API REST, using Clojure, Ring/Reitit and next-jdbc."
                                         :version "1.0.0"}}
                        :handler (swagger/create-swagger-handler)}}]
     ["/todos" {:get {:summary "Retrieves the collection of Todo resources."
                      :handler (fn [req] (ok (map #(append-todo-url % req) (store/get-all-todos db))))}
                :post {:summary "Creates a Todo resource."
                       :handler (fn [{:keys [body-params] :as req}] (-> body-params
                                                                       (store/create-todos db)
                                                                       (append-todo-url req)
                                                                       ok))}
                :delete {:summary "Removes all Todo resources"
                         :handler (fn [_] (store/delete-all-todos db)
                                    {:status 204})}
                :options (fn [_] {:status 200})}]
     ["/todos/:id" {:parameters {:path {:id s/Int}}
                    :get {:summary "Retrieves a Todo resource."
                          :handler (fn [{:keys [parameters] :as req}] (-> (store/get-todo (get-in parameters [:path :id]) db)
                                                                         (append-todo-url req)
                                                                         ok))}
                    :patch {:summary "Updates the Todo resource."
                            :handler (fn [{:keys [parameters body-params] :as req}] (-> body-params
                                                                                       (store/update-todo (get-in parameters [:path :id]) db)
                                                                                       (append-todo-url req)
                                                                                       ok))}
                    :delete {:summary "Removes the Todo resource."
                             :handler (fn [{:keys [parameters]}] (store/delete-todos (get-in parameters [:path :id]) db)
                                        {:status 204})}}]]
    {:data {:muuntaja m/instance
            :coercion rcs/coercion
            :middleware [rrmm/format-middleware
                         rrc/coerce-exceptions-middleware
                         rrc/coerce-response-middleware
                         rrc/coerce-request-middleware
                         [wrap-cors :access-control-allow-origin [#".*"]
                                    :access-control-allow-methods [:get :put :post :patch :delete]]]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/"}))
   (ring/create-default-handler
    {:not-found (constantly {:status 404 :body "Not found"})})))

(defn -main [& _]
  (clip/start system-config)
  @(promise))
