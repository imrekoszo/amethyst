;
; Copyright (c) 2019 the original author or authors.
; Licensed under the Eclipse Public License 2.0
; which is available at http://www.eclipse.org/legal/epl-2.0
;

#_(ns not-ns-form)

(ns form-before-ns
  "doc string"
  (:require
   [clojure.core.async :as async :refer [<! <!! >! >!!]]
   [clojure.tools.logging :refer :all]))

(defn some-func []
  (comment "do something"))
