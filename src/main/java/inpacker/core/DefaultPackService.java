package inpacker.core;

import java.io.File;
import java.util.Map;
import java.util.concurrent.*;

public class DefaultPackService<C extends PackConfig<I>, I extends PackItem> {

    private final Repository<C, I> repository;
    private final Packer<I> packer;
    private final Map<String, Pack> packs;
    private final File packsDir;
    private final ExecutorService executorService;

    public DefaultPackService(File packsDirectory, Repository<C, I> repository, Packer<I> packer) {
        packs = new ConcurrentHashMap<>();
        this.repository = repository;
        this.packer = packer;
        packsDir = packsDirectory;
        packsDir.mkdirs();
        this.executorService = Executors.newCachedThreadPool();
    }

    public Pack createPack(C config) {
        final String packName = config.getPackName();
        if (packs.containsKey(packName)) return packs.get(packName);
        final Pack pack = new Pack(packName);
        packs.put(packName, pack);
        final BlockingDeque<I> deque = new LinkedBlockingDeque<>();
        executorService.submit(() -> repository.getPackItems(config, deque));
        pack.processing();
        executorService.submit(() -> packer.pack(deque, packsDir, pack));
        return pack;
    }

    public Pack getPack(String packName) {
        return packs.get(packName);
    }

    public File getPackFile(String packName) {
        final Pack pack = packs.get(packName);
        if (pack == null || !pack.isDone()) return null;
        else return new File(packsDir, packName + ".zip");
    }
}
