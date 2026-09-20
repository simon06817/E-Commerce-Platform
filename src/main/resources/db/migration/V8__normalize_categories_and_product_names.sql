-- Keep category ids stable for existing references while replacing the old
-- phone/computer hierarchy with five top-level product categories.
UPDATE `product_category`
SET `name` = CASE `id`
        WHEN 1 THEN '电子产品'
        WHEN 2 THEN '图书'
        WHEN 3 THEN '服装'
        WHEN 4 THEN '玩具'
        WHEN 5 THEN '食品'
    END,
    `parent_id` = 0,
    `sort_order` = `id`
WHERE `id` BETWEEN 1 AND 5;

-- The original phone and computer products belong to the electronics category
-- after category 4 and 5 are repurposed.
UPDATE `product`
SET `category_id` = 1
WHERE `id` IN (1, 2)
  AND `category_id` IN (4, 5);

-- Convert generated demo product codes to readable names based on category.
DROP TEMPORARY TABLE IF EXISTS `tmp_product_name_map`;
CREATE TEMPORARY TABLE `tmp_product_name_map` (
    `product_id`  bigint       NOT NULL,
    `category_id` bigint       NOT NULL,
    `new_name`    varchar(100) NOT NULL,
    PRIMARY KEY (`product_id`)
) ENGINE = InnoDB;

INSERT INTO `tmp_product_name_map` (`product_id`, `category_id`, `new_name`)
SELECT
    `id`,
    `category_id`,
    COALESCE(
        CASE `category_id`
            WHEN 1 THEN ELT(
                `category_rank`,
                '无线蓝牙耳机', '智能运动手表', '便携式充电宝', '机械键盘',
                '无线静音鼠标', '蓝牙桌面音箱', '智能健康手环', 'USB-C 快充充电器',
                '高清平板电脑', '智能安防摄像头', '智能指纹门锁', '降噪运动耳机',
                '无线游戏手柄', '便携式显示器', '智能语音音箱', '高清电脑摄像头',
                'NVMe 固态硬盘', '双频无线路由器', '移动固态硬盘', '无线领夹麦克风'
            )
            WHEN 2 THEN ELT(
                `category_rank`,
                '深入理解计算机系统', '算法导论', '数据库系统概念', 'Spring Boot 实战',
                '代码整洁之道', '计算机网络自顶向下方法', '操作系统导论',
                '数据结构与算法分析', 'Java 并发编程实战', 'MySQL 技术内幕',
                'Redis 设计与实现', '设计模式：可复用面向对象软件的基础',
                '重构：改善既有代码的设计', '人月神话', '程序员修炼之道',
                '网络安全基础', '软件工程实践', 'Python 编程从入门到实践',
                '人工智能导论'
            )
            WHEN 3 THEN ELT(
                `category_rank`,
                '男士纯棉短袖T恤', '女士休闲长袖衬衫', '经典直筒牛仔裤',
                '轻薄防风夹克', '舒适运动卫衣', '保暖针织毛衣', '女士高腰半身裙',
                '商务修身西装外套', '夏季透气休闲裤', '儿童纯棉卫衣',
                '羊毛混纺大衣', '运动速干短裤', '女士时尚连衣裙', '男士商务衬衫',
                '休闲连帽外套', '高弹瑜伽裤', '复古格子衬衫', '冬季加厚羽绒服',
                '柔软家居睡衣套装'
            )
            WHEN 4 THEN ELT(
                `category_rank`,
                '儿童益智积木套装', '遥控越野赛车', '迷你拼装工程车', '创意磁力片玩具',
                '智能互动机器狗', '儿童绘画美术套装', '木质拼图益智玩具',
                '遥控飞行无人机', '仿真厨房玩具套装', '轨道赛车套装',
                '儿童科学实验套件', '可动人偶模型', '益智桌游棋类套装',
                '儿童音乐电子琴', '沙滩戏水玩具套装', '恐龙模型玩具套装',
                '儿童手工黏土套装', '遥控特技机器人', '户外泡泡机玩具'
            )
            WHEN 5 THEN ELT(
                `category_rank`,
                '每日混合坚果礼盒', '手工黄油曲奇饼干', '精品阿拉比卡咖啡豆',
                '有机全脂纯牛奶', '黑巧克力礼盒', '五常稻花香大米',
                '天然蜂蜜柚子茶', '传统手工桃酥', '高蛋白燕麦能量棒',
                '原味酸奶果粒麦片', '海盐苏打饼干', '新疆红枣夹核桃',
                '精选龙井绿茶', '低温烘焙腰果仁', '法式马卡龙甜点',
                '天然蔓越莓果干', '日式海苔脆片', '黑芝麻核桃粉',
                '轻食鸡胸肉即食包'
            )
        END,
        CONCAT('精选商品-', LPAD(`category_rank`, 2, '0'))
    )
FROM (
    SELECT
        `id`,
        `category_id`,
        ROW_NUMBER() OVER (PARTITION BY `category_id` ORDER BY `id`) AS `category_rank`
    FROM `product`
    WHERE `name` REGEXP '^Product-[0-9]+-[0-9]+$'
      AND `category_id` BETWEEN 1 AND 5
) AS ranked;

UPDATE `product` AS p
JOIN `tmp_product_name_map` AS m ON m.`product_id` = p.`id`
SET p.`name` = m.`new_name`
WHERE p.`name` REGEXP '^Product-[0-9]+-[0-9]+$';

DROP TEMPORARY TABLE IF EXISTS `tmp_product_name_map`;
