/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.starter.file.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vip.mate.starter.file.spi.FileStorage;

/**
 * Holds all {@link FileStorage} providers keyed by {@link FileStorage#type()}.
 * Spring injects every provider bean; lookup is by resolved type.
 *
 * @author mateaix
 */
public class FileStorageRegistry {

    private final Map<String, FileStorage> byType = new LinkedHashMap<>();

    public FileStorageRegistry(List<FileStorage> storages) {
        if (storages != null) {
            for (FileStorage storage : storages) {
                byType.put(storage.type(), storage);
            }
        }
    }

    /** Get the provider for {@code type}, or throw if none is registered. */
    public FileStorage get(String type) {
        FileStorage storage = byType.get(type);
        if (storage == null) {
            throw new FileStorageException("No file storage provider registered for type '" + type
                    + "'. Registered: " + byType.keySet());
        }
        return storage;
    }

    public boolean has(String type) {
        return byType.containsKey(type);
    }

    /** All registered providers (for descriptor listing). */
    public Collection<FileStorage> all() {
        return byType.values();
    }
}
