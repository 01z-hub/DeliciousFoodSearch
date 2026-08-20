# DeliciousFoodSearch

“周末吃什么”是一款帮助情侣发现附近美食、比较价格和团购的 Android 应用。

## 当前已实现

- 用户主动授权后的设备定位。
- 拒绝定位时继续使用明确说明的演示位置。
- 附近商家、菜品和团购列表。
- 价格从低到高、味道从高到低、距离从低到高三种排序。
- 点击“更新商家与价格”后刷新商家状态、菜品价格、团购价格、距离和更新时间。
- 加载、成功、失败、重试、空数据和历史数据降级状态。

## 数据说明

当前商家、菜品、评价和团购均为界面中明确标注的演示数据，不代表抖音、美团或任何真实商家信息。

抖音生活服务商品查询接口面向商家或服务商，需要申请接口权限和商家授权。项目不会在 Android 客户端保存平台密钥，也不会抓取平台页面。获得合法授权后，可以实现 `FoodDiscoveryDataSource`，并通过自有服务端返回合规数据，替换当前的 `DemoFoodDataSource`。

## 构建

项目需要 JDK 17 或更高版本以及 Android SDK 37：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

调试 APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

