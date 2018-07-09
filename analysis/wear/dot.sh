#!/bin/bash

echo stg.dot
dot -Tpdf stg.dot -o stg.pdf

echo flowgraph.dot
dot -Tpdf flowgraph.dot -o flowgraph.pdf

