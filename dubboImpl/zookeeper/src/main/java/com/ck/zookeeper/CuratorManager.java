package com.ck.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 * @Author: ck
 * @CreateTime: 2025-06-20
 * @Description: zookeeper客户端管理器
 * @Version: 1.0
 */
public class CuratorManager {
    private CuratorFramework client;

    //创建客户端连接
    public void start(){
        //重试策略
        ExponentialBackoffRetry retry = new ExponentialBackoffRetry(1000, 3);

        client = CuratorFrameworkFactory.builder()
                .connectString("192.168.31.91:2181")
                .sessionTimeoutMs(30000)
                .connectionTimeoutMs(10000)
                .retryPolicy(retry)
                .namespace("myapp")
                .build();
        client.start();
        System.out.println("curator客户端启动成功");
    }

    //创建节点
    public void createNode() throws Exception {
        // 检查并创建持久节点
        String configPath = "/config/database";
        if (client.checkExists().forPath(configPath) == null) {
            client.create()
                    .creatingParentsIfNeeded() //自动创建父节点
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(configPath, "mysql://localhost:3306".getBytes());
            System.out.println("✅ 创建持久节点: " + configPath);
        } else {
            System.out.println("ℹ️ 持久节点已存在: " + configPath);
        }

        //创建临时有序节点
        String actualPath = client.create()
                .creatingParentsIfNeeded() //自动创建父节点
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath("/services/user-service-", "192.168.1.10:8080".getBytes());

        System.out.println("✅ 创建临时有序节点: " + actualPath);
    }

    //读取数据
    public void readData() throws Exception {
        String configPath = "/config/database";
        if (client.checkExists().forPath(configPath) != null) {
            byte[] data = client.getData().forPath(configPath);
            System.out.println("📖 数据库配置: " + new String(data));

            //获取节点状态
            Stat stat = new Stat();
            byte[] dataWithStat = client.getData().storingStatIn(stat).forPath(configPath);
            System.out.println("📊 节点版本: " + stat.getVersion());
        } else {
            System.out.println("❌ 节点不存在: " + configPath);
        }
    }

    //更新数据
    public void updateData() throws Exception {
        String configPath = "/config/database";
        if (client.checkExists().forPath(configPath) != null) {
            client.setData()
                    .withVersion(-1)
                    .forPath(configPath, "mysql://192.168.1.100:3306".getBytes());
            System.out.println("🔄 更新节点数据: " + configPath);
        } else {
            System.out.println("❌ 无法更新，节点不存在: " + configPath);
        }
    }

    //删除节点
    public void deleteNode() throws Exception {
        String configPath = "/config";
        if (client.checkExists().forPath(configPath) != null) {
            client.delete()
                    .guaranteed()  //保证删除成功
                    .deletingChildrenIfNeeded()  //递归删除子节点
                    .forPath(configPath);
            System.out.println("🗑️ 删除节点: " + configPath);
        } else {
            System.out.println("ℹ️ 节点不存在，无需删除: " + configPath);
        }
    }

    //监听子节点变化
    public void watchChildren() throws Exception {
        String servicesPath = "/services";

        // 确保监听的父节点存在
        if (client.checkExists().forPath(servicesPath) == null) {
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(servicesPath);
            System.out.println("✅ 创建监听父节点: " + servicesPath);
        }

        PathChildrenCache cache = new PathChildrenCache(client, servicesPath, true);

        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
                switch (event.getType()){
                    case CHILD_ADDED:
                        System.out.println("➕ 新增子节点: " + event.getData().getPath());
                        break;
                    case CHILD_UPDATED:
                        System.out.println("🔄 子节点更新: " + event.getData().getPath());
                        break;
                    case CHILD_REMOVED:
                        System.out.println("➖ 删除子节点: " + event.getData().getPath());
                        break;
                    default:
                        break;
                }
            }
        });

        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        System.out.println("👀 开始监听子节点变化: " + servicesPath);
    }

    // 📊 NodeCache - 监听单个节点数据变化
    public void watchNode() throws Exception {
        String configPath = "/config/database";

        if (client.checkExists().forPath(configPath) == null) {
            System.out.println("❌ 监听节点不存在: " + configPath);
            return;
        }

        NodeCache cache = new NodeCache(client, configPath);

        cache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                ChildData currentData = cache.getCurrentData();
                if(currentData != null){
                    System.out.println("🔔 节点数据变化: " + new String(currentData.getData()));
                }else{
                    System.out.println("🗑️ 节点被删除");
                }
            }
        });

        cache.start(true);  // 立即缓存数据
        System.out.println("👀 开始监听节点数据变化: " + configPath);
    }

    // 清理所有测试节点
    public void cleanupTestNodes() {
        try {
            // 清理可能存在的测试节点
            String[] pathsToClean = {"/config", "/services"};

            for (String path : pathsToClean) {
                if (client.checkExists().forPath(path) != null) {
                    client.delete()
                            .guaranteed()
                            .deletingChildrenIfNeeded()
                            .forPath(path);
                    System.out.println("🧹 清理节点: " + path);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ 清理节点时出现错误: " + e.getMessage());
        }
    }

    // 🔚 关闭客户端
    public void close() {
        if (client != null) {
            client.close();
            System.out.println("👋 Curator客户端已关闭");
        }
    }

    public static void main(String[] args) {
        CuratorManager manager = new CuratorManager();
        try {
            // 启动客户端
            manager.start();

            // 等待连接建立
            Thread.sleep(2000);

            // 可选：清理之前的测试节点
            // manager.cleanupTestNodes();
            // Thread.sleep(1000);

            // 基础操作演示
            manager.createNode();
            manager.readData();
            manager.updateData();
            manager.readData(); // 再次读取以查看更新后的数据

            // 监听演示
            manager.watchChildren();
            manager.watchNode();

            // 模拟一些变化来触发监听器
            System.out.println("\n🔄 开始模拟节点变化...");
            Thread.sleep(2000);

            // 创建一个新的临时节点来触发子节点监听
            String newService = manager.client.create()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath("/services/test-service-", "test-data".getBytes());
            System.out.println("🆕 创建测试服务节点: " + newService);

            // 更新数据来触发节点监听
            Thread.sleep(1000);
            manager.client.setData()
                    .forPath("/config/database", "mysql://updated:3306".getBytes());
            System.out.println("🔄 更新数据库配置");

            // 保持运行一段时间以观察监听效果
            Thread.sleep(5000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.close();
        }
    }
}
