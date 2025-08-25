package course.concurrency.m2_async.minPrice;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private ExecutorService executor = Executors.newCachedThreadPool();
    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        // Для каждого магазина создаём асинхронный запрос с таймаутом и обработкой исключений
        List<CompletableFuture<Double>> futures = shopIds.stream()
                .map(shopId -> CompletableFuture
                        .supplyAsync(() -> priceRetriever.getPrice(shopId, itemId), executor)
                        .completeOnTimeout(Double.POSITIVE_INFINITY, 2900, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> Double.POSITIVE_INFINITY)
                )
                .toList();

        // Из всех завершённых задач получаем значения, фильтруем ошибки/таймауты и ищем минимум
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Double::isFinite)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(Double.NaN);
    }
}
