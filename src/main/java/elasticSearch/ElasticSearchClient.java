/**
 * 
 */
package elasticSearch;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.search.Query;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * ElasticSearch测试
 * @author Admin
 *
 */
public class ElasticSearchClient {
	private static TransportClient transportClient;
	private static String clusterName = "zouxd-elasticSearch";
	private static String elasticSearchAddress = "localhost";
	private static String port = "9300";
	private static BulkProcessor bulkProcessor = null;

	/**
	 * BulkProcessor 初始化
	 * @return
	 */
	public static BulkProcessor initBulkProcessor(){
		if(null == bulkProcessor){
			bulkProcessor = BulkProcessor.builder(ElasticSearchClient.newClient(), new Listener() {
				public void beforeBulk(long executionId, BulkRequest request) {
					// TODO Auto-generated method stub
				}
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					// TODO Auto-generated method stub
					if(null != failure){
						System.out.println("bulk失败========================="+failure.getMessage());
					}
				}
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
					// TODO Auto-generated method stub
					System.out.println("executionId================="+executionId+",response==================="+response.status().getStatus());
				}
			}).setBulkActions(1000).setBulkSize(new ByteSizeValue(1,ByteSizeUnit.MB)).setFlushInterval(TimeValue.timeValueSeconds(20)).setConcurrentRequests(5).setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(500), 3)).build();
		}
		return bulkProcessor;
	}

	/**
	 * 获取client
	 * @return
	 */
	public static TransportClient newClient(){
		if(null == transportClient){
			Settings settings = Settings.builder().put("cluster.name", clusterName).build();
			transportClient = new PreBuiltTransportClient(settings);
			transportClient.addTransportAddress(new TransportAddress(new InetSocketAddress(elasticSearchAddress, Integer.parseInt(port))));
			//添加集群节点2
			transportClient.addTransportAddress(new TransportAddress(new InetSocketAddress(elasticSearchAddress, 9301)));
			System.out.println("ElasticSearch Client初始化成功");
		}
		return transportClient;
	}

	/**
	 * 获取集群下的所有索引
	 * @return
	 */
	public static String[] getAllindices(){
		String[] indices = ElasticSearchClient.newClient().admin().indices().prepareGetIndex().get().getIndices();
		for(String index:indices){
			System.out.println(index);
		}
		return indices;
	}

	/**
	 * 删除指定索引
	 * @param indexName
	 * @return
	 */
	public static boolean deleteIndex(String indexName){
		IndicesExistsRequest indicesExistsRequest = new IndicesExistsRequest(indexName);
		IndicesExistsResponse response= ElasticSearchClient.newClient().admin().indices().exists(indicesExistsRequest).actionGet();
		if(!response.isExists()){
			return false;
		}
		DeleteIndexResponse deleteIndexResponse = ElasticSearchClient.newClient().admin().indices().prepareDelete(indexName).get();
		System.out.println(indexName+"删除==========================="+(deleteIndexResponse.isAcknowledged()?"成功":"失败"));
		return deleteIndexResponse.isAcknowledged();
	}

	/**
	 * 判断指定索引是否存在
	 * @param indexName
	 * @return
	 */
	public static boolean isIndexExist(String indexName){
		IndicesExistsResponse indicesExistsResponse = ElasticSearchClient.newClient().admin().indices().prepareExists(indexName).get();
		return indicesExistsResponse.isExists();
	}

	/**
	 * 创建索引
	 * @param indexName
	 * @return
	 * @throws IOException
	 */
	public static boolean createIndex(String indexName) throws IOException{
		if(ElasticSearchClient.isIndexExist(indexName)){
			System.out.println(indexName+"已存在===========================");
			return false;
		}
//		String settings ="{\"settings\":{\"translog.flush_threshold_size\":\"1KB\",\"refresh_interval\":\"10s\",\"number_of_shards\":1,\"number_of_replicas\":1,\"analysis\":{\"char_filter\":{\"my_char_filter\":{\"type\":\"mapping\",\"mappings\":[\"\\.=>''\",\"\\:=>''\"]}},\"analyzer\":{\"my_analyzer\":{\"type\":\"custom\",\"char_filter\":[\"my_char_filter\"],\"filter\":[\"lowercase\"],\"tokenizer\":\"standard\"}}}}}";
//		XContent xContent = XContentFactory.xContent(settings.getBytes());
//		XContentBuilder source = XContentBuilder.builder(xContent);
//		String mapping = "{\"properties\":{\"brithday\":{\"type\":\"date\"},\"id\":{\"type\":\"keyword\"},\"sexy\":{\"type\":\"text\",\"fielddata\":true},\"msg\":{\"type\":\"text\",\"analyzer\":\"my_analyzer\"},\"ename\":{\"type\":\"keyword\"},\"name\":{\"type\":\"text\"},\"info\":{\"type\":\"text\",\"analyzer\":\"my_analyzer\"}}}";
//		CreateIndexResponse createIndexResponse = ElasticSearchClient.newClient().admin().indices().prepareCreate(indexName).setSource(source).addMapping("collage",mapping).get();
		CreateIndexResponse createIndexResponse = ElasticSearchClient.newClient().admin().indices().prepareCreate(indexName).get();
		return createIndexResponse.isAcknowledged();
	}

	/**
	 * 保存相关数据到指定索引执行type下
	 * @param indexName
	 * @param type
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static boolean saveToES(String indexName,String type,Object data) throws IOException{
		if(!ElasticSearchClient.isIndexExist(indexName)){
			if(ElasticSearchClient.createIndex(indexName)){
				System.out.println("==========================="+indexName+"创建成功！！！");
			}
		}
		IndexResponse indexResponse = ElasticSearchClient.newClient().prepareIndex(indexName, type, UUID.randomUUID().toString()).setSource(data.toString().getBytes(), XContentType.JSON).get();
		long version = indexResponse.getVersion();
		if(version>0){
			System.out.println("===========================数据保存成功！！！");
			return true;
		}
		return false;
	}
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		ElasticSearchClient.getAllindices();
		System.out.println(ElasticSearchClient.deleteIndex("student"));
		ElasticSearchClient.getAllindices();
		System.out.println("===========================");
//		ElasticSearchClient.createIndex("student");
		System.out.println("===========================");
		ElasticSearchClient.getAllindices();
		try {
			long start = System.currentTimeMillis();
			for(int i=0;i<100000;i++){
				Student temp = new Student();
				temp.setId((long)i);
				temp.setEname("ename_"+Math.random());
				temp.setName("小明_"+i);
				String date = formatDateToString(new Date());
				date = date.replaceAll("\\s", "T")+"Z";
				temp.setBrithday(date);
				temp.setSexy(Math.random()>= 0.5?"male":"female");
				temp.setInfo("<breakfast_menu><food><name>Belgian Waffles</name><price>$5.95</price><description>two of our famous Belgian Waffles with plenty of real maple syrup</description><calories>650</calories></food><food><name>Strawberry Belgian Waffles</name><price>$7.95</price><description>light Belgian waffles covered with strawberries and whipped cream</description><calories>900</calories></food><food><name>Berry-Berry Belgian Waffles</name><price>$8.95</price><description>light Belgian waffles covered with an assortment of fresh berries and whipped cream</description><calories>900</calories></food><food><name>French Toast</name><price>$4.50</price><description>thick slices made from our homemade sourdough bread</description><calories>600</calories></food><food><name>Homestyle Breakfast</name><price>$6.95</price><description>two eggs, bacon or sausage, toast, and our ever-popular hash browns</description><calories>950</calories></food></breakfast_menu>");
//				ElasticSearchClient.saveToES("student", "collage", temp);
				IndexRequest request = ElasticSearchClient.newClient().prepareIndex("student", "collage").setId(UUID.randomUUID().toString()).setSource(temp.toString().getBytes(), XContentType.JSON).request();
				ElasticSearchClient.initBulkProcessor().add(request);
			}
			long end = System.currentTimeMillis();
			System.out.println("=============完成保存==============耗时："+(end-start)/1000+"s");
		} catch (Exception e) {
			e.printStackTrace();
		}
		ElasticSearchClient.search();
	}

	/**
	 * 查询相关范围内数据(from小于10000)
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static void search() throws ExecutionException, InterruptedException {
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchSourceBuilder.query(ElasticSearchClient.queryBuilder());
//		searchSourceBuilder.from(1000).size(10);
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.indices("student").types("collage").source(searchSourceBuilder);
		SearchResponse searchResponse = ElasticSearchClient.newClient().search(searchRequest).get();
		System.out.println("查询状态==================================="+searchResponse.status());
		SearchHits searchHits = searchResponse.getHits();
		SearchHit[] searchHitArray = searchHits.getHits();
		System.out.println("总条数========================="+searchResponse.getHits().totalHits);
		for(SearchHit searchHit:searchHitArray){
			String source= searchHit.getSourceAsString();
			String id = searchHit.getId();
			SearchShardTarget searchShardTarget = searchHit.getShard();
			ShardId shardId = searchShardTarget.getShardId();
			int shardIdId = shardId.getId();
			String indexName = shardId.getIndexName();
			System.out.println("id========================="+id);
			System.out.println("shardId========================="+shardId+",indexName=============="+indexName);
			System.out.println("source========================="+source);
		}
	}

	/**
	 * 返回QueryBuilder
	 * @return
	 */
	public static QueryBuilder queryBuilder(){
		//字段范围查询
//		QueryBuilder queryBuilder = QueryBuilders.rangeQuery("id").from(10000).to(10100).includeLower(false).includeUpper(true);
		//按照过滤条件查询
		QueryBuilder queryBuilder = QueryBuilders.boolQuery().
				filter(QueryBuilders.termQuery("sexy","female")).
				must(QueryBuilders.rangeQuery("id").from(10000).to(10100).includeLower(false).includeUpper(true)).
				mustNot(QueryBuilders.termQuery("id","10091"));
//		AggregationBuilder aggregationBuilder = AggregationBuilders.terms("sexy").field("sexy");
		return queryBuilder;
	}
	/**
	 * 日期转string
	 * @param date
	 * @return
	 */
	private static String formatDateToString(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
		return sdf.format(date);
	}
}
