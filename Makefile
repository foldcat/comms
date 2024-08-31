.PHONY: repl
repl:
	clj -M:cider

.PHONY: deps
deps:
	clojure -e "(println \"Dependencies downloaded\")"
