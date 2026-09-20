USE `E-Commerce_Platform`;
SET NAMES utf8mb4;

-- BCrypt hash for password: 123456
SET @demo_password = '$2a$10$CybQusAxxBrkYBSVzIOCIeV4P6l5AelPOxJtNrf4FZW4u9RFvIHba';

-- Buyers 03-50 (existing buyer01/buyer02 keep their ids)
INSERT IGNORE INTO `user_buyer` (`username`, `password`, `nickname`, `phone`, `email`, `address`)
WITH RECURSIVE seq(n) AS (
    SELECT 3
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CONCAT('buyer', LPAD(n, 2, '0')),
    @demo_password,
    CONCAT('buyer ', n),
    CONCAT('1380000', LPAD(n, 4, '0')),
    CONCAT('buyer', LPAD(n, 2, '0'), '@example.com'),
    'Demo City'
FROM seq;

-- Sellers 03-10 (existing seller01/seller02 keep their ids)
INSERT IGNORE INTO `user_seller` (`username`, `password`, `shop_name`, `phone`, `email`)
WITH RECURSIVE seq(n) AS (
    SELECT 3
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 10
)
SELECT
    CONCAT('seller', LPAD(n, 2, '0')),
    @demo_password,
    CONCAT('Shop ', LPAD(n, 2, '0')),
    CONCAT('1390000', LPAD(n, 4, '0')),
    CONCAT('seller', LPAD(n, 2, '0'), '@example.com')
FROM seq;

-- Add 96 products so that every seller has 10 products.
-- Existing 4 products: seller01 has 4, seller02 has 0.
-- Final category distribution: 22/20/20/19/19 for categories 1-5.
INSERT INTO `product` (`seller_id`, `name`, `description`, `price`, `stock`,
                       `category_id`, `main_image`, `status`)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 96
),
ranked AS (
    SELECT
        n,
        CASE
            WHEN n <= 6 THEN 1
            WHEN n <= 16 THEN 2
            ELSE 2 + CEIL((n - 16) / 10)
        END AS seller_id,
        CASE
            WHEN n <= 20 THEN 1
            WHEN n <= 39 THEN 2
            WHEN n <= 58 THEN 3
            WHEN n <= 77 THEN 4
            ELSE 5
        END AS category_id
    FROM seq
),
numbered AS (
    SELECT
        n,
        seller_id,
        category_id,
        ROW_NUMBER() OVER (PARTITION BY category_id ORDER BY n) AS category_rank
    FROM ranked
),
named AS (
    SELECT
        n,
        seller_id,
        category_id,
        category_rank,
        CASE category_id
            WHEN 1 THEN ELT(
                category_rank,
                '无线蓝牙耳机', '智能运动手表', '便携式充电宝', '机械键盘',
                '无线静音鼠标', '蓝牙桌面音箱', '智能健康手环', 'USB-C 快充充电器',
                '高清平板电脑', '智能安防摄像头', '智能指纹门锁', '降噪运动耳机',
                '无线游戏手柄', '便携式显示器', '智能语音音箱', '高清电脑摄像头',
                'NVMe 固态硬盘', '双频无线路由器', '移动固态硬盘', '无线领夹麦克风'
            )
            WHEN 2 THEN ELT(
                category_rank,
                '深入理解计算机系统', '算法导论', '数据库系统概念', 'Spring Boot 实战',
                '代码整洁之道', '计算机网络自顶向下方法', '操作系统导论',
                '数据结构与算法分析', 'Java 并发编程实战', 'MySQL 技术内幕',
                'Redis 设计与实现', '设计模式：可复用面向对象软件的基础',
                '重构：改善既有代码的设计', '人月神话', '程序员修炼之道',
                '网络安全基础', '软件工程实践', 'Python 编程从入门到实践',
                '人工智能导论'
            )
            WHEN 3 THEN ELT(
                category_rank,
                '男士纯棉短袖T恤', '女士休闲长袖衬衫', '经典直筒牛仔裤',
                '轻薄防风夹克', '舒适运动卫衣', '保暖针织毛衣', '女士高腰半身裙',
                '商务修身西装外套', '夏季透气休闲裤', '儿童纯棉卫衣', '羊毛混纺大衣',
                '运动速干短裤', '女士时尚连衣裙', '男士商务衬衫', '休闲连帽外套',
                '高弹瑜伽裤', '复古格子衬衫', '冬季加厚羽绒服', '柔软家居睡衣套装'
            )
            WHEN 4 THEN ELT(
                category_rank,
                '儿童益智积木套装', '遥控越野赛车', '迷你拼装工程车', '创意磁力片玩具',
                '智能互动机器狗', '儿童绘画美术套装', '木质拼图益智玩具',
                '遥控飞行无人机', '仿真厨房玩具套装', '轨道赛车套装',
                '儿童科学实验套件', '可动人偶模型', '益智桌游棋类套装',
                '儿童音乐电子琴', '沙滩戏水玩具套装', '恐龙模型玩具套装',
                '儿童手工黏土套装', '遥控特技机器人', '户外泡泡机玩具'
            )
            WHEN 5 THEN ELT(
                category_rank,
                '每日混合坚果礼盒', '手工黄油曲奇饼干', '精品阿拉比卡咖啡豆',
                '有机全脂纯牛奶', '黑巧克力礼盒', '五常稻花香大米',
                '天然蜂蜜柚子茶', '传统手工桃酥', '高蛋白燕麦能量棒',
                '原味酸奶果粒麦片', '海盐苏打饼干', '新疆红枣夹核桃',
                '精选龙井绿茶', '低温烘焙腰果仁', '法式马卡龙甜点',
                '天然蔓越莓果干', '日式海苔脆片', '黑芝麻核桃粉',
                '轻食鸡胸肉即食包'
            )
        END AS product_name
    FROM numbered
)
SELECT
    seller_id,
    product_name,
    CASE category_id
        WHEN 1 THEN CONCAT(product_name, '，做工扎实，性能稳定，适合办公、学习、通勤与日常娱乐使用。')
        WHEN 2 THEN CONCAT(product_name, '，正版精选图书，内容系统全面，适合课程学习、技术提升与日常阅读。')
        WHEN 3 THEN CONCAT(product_name, '，选用舒适面料，版型合身，适合日常通勤、休闲与四季穿搭。')
        WHEN 4 THEN CONCAT(product_name, '，采用安全环保设计，兼具趣味性和益智性，适合亲子互动与儿童娱乐。')
        WHEN 5 THEN CONCAT(product_name, '，精选优质原料制作，口感丰富，适合日常食用和家庭分享。')
    END,
    CASE category_id
        WHEN 1 THEN ELT(
            category_rank,
            199.00, 699.00, 129.00, 399.00, 99.00, 299.00, 249.00, 89.00,
            2999.00, 199.00, 1299.00, 499.00, 329.00, 1099.00, 399.00, 199.00,
            599.00, 249.00, 499.00, 299.00
        )
        WHEN 2 THEN ELT(
            category_rank,
            139.00, 128.00, 99.00, 79.00, 59.00, 89.00, 79.00, 69.00,
            89.00, 89.00, 79.00, 99.00, 99.00, 69.00, 79.00, 59.00,
            69.00, 89.00, 59.00
        )
        WHEN 3 THEN ELT(
            category_rank,
            69.00, 129.00, 199.00, 299.00, 159.00, 229.00, 169.00, 499.00,
            139.00, 99.00, 699.00, 99.00, 259.00, 199.00, 269.00, 129.00,
            159.00, 899.00, 199.00
        )
        WHEN 4 THEN ELT(
            category_rank,
            199.00, 299.00, 89.00, 159.00, 499.00, 129.00, 59.00, 399.00,
            229.00, 259.00, 169.00, 99.00, 139.00, 299.00, 79.00, 129.00,
            69.00, 349.00, 59.00
        )
        WHEN 5 THEN ELT(
            category_rank,
            99.00, 49.00, 88.00, 69.00, 79.00, 129.00, 59.00, 39.00,
            49.00, 45.00, 19.90, 59.00, 128.00, 79.00, 98.00, 45.00,
            29.90, 49.00, 39.90
        )
    END,
    10 + MOD(n, 90),
    category_id,
    NULL,
    1
FROM named
WHERE NOT EXISTS (
    SELECT 1
    FROM `product`
    WHERE `description` LIKE 'Demo product for category %'
);

-- Verification queries
SELECT 'buyer_count' AS metric, COUNT(*) AS value FROM `user_buyer`
UNION ALL
SELECT 'seller_count', COUNT(*) FROM `user_seller`
UNION ALL
SELECT 'product_count', COUNT(*) FROM `product`;

SELECT seller_id, COUNT(*) AS product_count
FROM `product`
GROUP BY seller_id
ORDER BY seller_id;

SELECT category_id, COUNT(*) AS product_count
FROM `product`
GROUP BY category_id
ORDER BY category_id;

SELECT id, username, password, phone, email, create_time
FROM `user_admin`;
