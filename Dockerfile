FROM python

RUN mkdir /tmp/databricks

RUN pip install databricks-cli

WORKDIR /tmp/databricks