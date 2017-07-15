/* Copyright (C) 2012 Yuvi Panda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.free.nrw.commons.libs.http_fluent;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.*;
import org.apache.http.Header;

public class CountingRequestEntity implements HttpEntity {
    private final HttpEntity delegate;

    private final ProgressListener listener;

    private final double PROGRESS_TRIGGER_PERCENTAGE;

    public CountingRequestEntity(final HttpEntity entity,
            final ProgressListener listener) {
        super();
        this.delegate = entity;
        this.listener = listener;
        // Percenetage of total size after which to trigger progress updates. Defaults to 0.5%
        this.PROGRESS_TRIGGER_PERCENTAGE = Double.valueOf(System.getProperty("in.yuvi.http.fluent.PROGRESS_TRIGGER_THRESHOLD", "0.5")) / 100;
    }

    public long getContentLength() {
        return this.delegate.getContentLength();
    }

    public boolean isRepeatable() {
        return this.delegate.isRepeatable();
    }

    public void writeTo(final OutputStream out) throws IOException {
        this.delegate.writeTo(new CountingOutputStream(out, this.listener, this.getContentLength(), PROGRESS_TRIGGER_PERCENTAGE));
    }

    public static class CountingOutputStream extends FilterOutputStream {

        private final ProgressListener listener;

        private long transferred;
        private long total;
        private long runningCount;
        private final long progressTriggerThreshold;

        public CountingOutputStream(final OutputStream out,
                final ProgressListener listener, final long totalLength, final double PROGRESS_TRIGGER_PERCENTAGE) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
            this.total = totalLength;
            this.runningCount = 0;
            this.progressTriggerThreshold = (long)(this.total *  PROGRESS_TRIGGER_PERCENTAGE);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            transferred += len;
            runningCount += len;
            if(runningCount >= progressTriggerThreshold || transferred == total) {
                listener.onProgress(transferred, total);
                runningCount = 0;
            }
        }

        public void write(int b) throws IOException {
            out.write(b);
            transferred++;
            runningCount++;
            if(runningCount >= progressTriggerThreshold || transferred == total) {
                this.listener.onProgress(transferred, total);
                runningCount = 0;
            }
        }
    }

    @Override
    @Deprecated
    public void consumeContent() throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        return delegate.getContent();
    }

    @Override
    public Header getContentEncoding() {
        return delegate.getContentEncoding();
    }

    @Override
    public boolean isChunked() {
        return delegate.isChunked();
    }

    @Override
    public boolean isStreaming() {
        return delegate.isStreaming();
    }

    @Override
    public Header getContentType() {
        return delegate.getContentType();
    }
}
