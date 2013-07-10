/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wikimedia.commons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import org.wikimedia.commons.contributions.Contribution;
import org.wikimedia.commons.contributions.ContributionsContentProvider;


public class MediaWikiImageView extends ImageView {

    private Media mMedia;

    private ImageLoader mImageLoader;

    private ImageContainer mImageContainer;

    private View loadingView;

    private boolean isThumbnail;

    public MediaWikiImageView(Context context) {
        this(context, null);
    }

    public MediaWikiImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        TypedArray actualAttrs = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MediaWikiImageView, 0, 0);
        isThumbnail = actualAttrs.getBoolean(0, false);
    }

    public MediaWikiImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMedia(Media media, ImageLoader imageLoader) {
        this.mMedia = media;
        mImageLoader = imageLoader;
        loadImageIfNecessary(false);
    }

    public void setLoadingView(View loadingView) {
        this.loadingView = loadingView;
    }

    public View getLoadingView() {
        return loadingView;
    }

    private void loadImageIfNecessary(final boolean isInLayoutPass) {
        loadImageIfNecessary(isInLayoutPass, false);
    }

    private void loadImageIfNecessary(final boolean isInLayoutPass, final boolean tryOriginal) {
        int width = getWidth();
        int height = getHeight();

        // if the view's bounds aren't known yet, hold off on loading the image.
        if (width == 0 && height == 0) {
            return;
        }

        if(mMedia == null) {
            return;
        }


        // Do not count for density when loading thumbnails.
        // FIXME: Use another 'algorithm' that doesn't punish low res devices
        if(isThumbnail) {
            float dpFactor =  Math.max(getResources().getDisplayMetrics().density, 1.0f);
            width = (int) (width / dpFactor);
            height = (int) (height / dpFactor);
        }

        final String  mUrl;
        if(tryOriginal) {
            mUrl = mMedia.getImageUrl();
        } else {
            // Round it to the nearest 320
            // Possible a similar size image has already been generated.
            // Reduces Server cache fragmentation, also increases chance of cache hit
            // If width is less than 320, we round up to 320
            int bucketedWidth = width <= 320 ? 320 : Math.round((float)width / 320.0f) * 320;
            if(mMedia.getWidth() != 0 && mMedia.getWidth() < bucketedWidth) {
                // If we know that the width of the image is lesser than the required width
                // We don't even try to load the thumbnai, go directly to the source
                loadImageIfNecessary(isInLayoutPass, true);
                return;
            } else {
                mUrl = mMedia.getThumbnailUrl(bucketedWidth);
            }
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setImageBitmap(null);
            return;
        }

        // Don't repeat work. Prevents onLayout cascades
        // We ignore it if the image request was for either the current URL of for the full URL
        // Since the full URL is always the second, and
        if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if (mImageContainer.getRequestUrl().equals(mMedia.getImageUrl()) || mImageContainer.getRequestUrl().equals(mUrl)) {
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mImageContainer.cancelRequest();
                BitmapDrawable actualDrawable = (BitmapDrawable)getDrawable();
                if(actualDrawable != null && actualDrawable.getBitmap() != null) {
                    setImageBitmap(null);
                    if(loadingView != null) {
                        loadingView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        ImageContainer newContainer = mImageLoader.get(mUrl,
                new ImageListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        if(!tryOriginal) {
                            post(new Runnable() {
                                public void run() {
                                    loadImageIfNecessary(false, true);
                                }
                            });
                        }

                    }

                    @Override
                    public void onResponse(final ImageContainer response, boolean isImmediate) {
                        // If this was an immediate response that was delivered inside of a layout
                        // pass do not set the image immediately as it will trigger a requestLayout
                        // inside of a layout. Instead, defer setting the image by posting back to
                        // the main thread.
                        if (isImmediate && isInLayoutPass) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    onResponse(response, false);
                                }
                            });
                            return;
                        }

                        if (response.getBitmap() != null) {
                            setImageBitmap(response.getBitmap());
                            if(tryOriginal && mMedia instanceof Contribution && response.getBitmap().getWidth() > mMedia.getWidth() || response.getBitmap().getHeight() > mMedia.getHeight()) {
                                // If there is no width information for this image, save it. This speeds up image loading massively for smaller images
                                mMedia.setHeight(response.getBitmap().getHeight());
                                mMedia.setWidth(response.getBitmap().getWidth());
                                ((Contribution)mMedia).setContentProviderClient(MediaWikiImageView.this.getContext().getContentResolver().acquireContentProviderClient(ContributionsContentProvider.AUTHORITY));
                                ((Contribution)mMedia).save();
                            }
                            if(loadingView != null) {
                                loadingView.setVisibility(View.GONE);
                            }
                        } else {
                            // I'm not really sure where this would hit but not onError
                        }
                    }
                });

        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mImageContainer.cancelRequest();
            setImageBitmap(null);
            // also clear out the container so we can reload the image if necessary.
            mImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
