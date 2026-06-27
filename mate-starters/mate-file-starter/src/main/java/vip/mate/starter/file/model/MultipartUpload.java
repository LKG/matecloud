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
package vip.mate.starter.file.model;

/**
 * Handle to an in-progress multipart (chunked) upload.
 *
 * <p>The client flow stays vendor-neutral: <em>init → per-part pre-signed PUT
 * (browser uploads each chunk directly to the object store) → complete</em>. The
 * bytes never transit the application server,
 * and an interrupted upload can resume by re-requesting URLs for the missing parts.
 *
 * @param bucket     bucket the object will land in (resolved, never blank)
 * @param objectName storage key (caller-supplied or generated at init)
 * @param uploadId   vendor upload id — pass it back on every part URL / complete / abort
 *
 * @author mateaix
 */
public record MultipartUpload(String bucket, String objectName, String uploadId) {
}
