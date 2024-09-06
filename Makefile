.PHONY: repl
repl:
	clj -M:cider

.PHONY: deps
deps:
	clojure -e "(println \"Dependencies downloaded\")"

.PHONY: test 
test:
	clj -X:test

.PHONY: outdated 
outdated: 
	clj -M:outdated
