AWS SQS River Plugin for ElasticSearch
==================================

The AWS SQS plugin uses Amazon's SQS as a river by pulling messages from a given queue. Right after a message is indexed it gets deleted from the queue. 

Messages are in the format `{"_id": "123", "_type": "es_data_type", "_data": {"key1":"value1"...}}`. If `_data` is missing the data with this id will be deleted from the index.

The fields `_id` and `_type` are required. 

To configure put this in your `elasticsearch.yml`:
    
    cloud.aws.region: region
    cloud.aws.access_key: key
    cloud.aws.secret_key: key
    cloud.aws.sqs.queue_url: url

In order to install the plugin, simply run: `bin/plugin -install aleski/elasticsearch-river-amazonsqs/1.1`.
