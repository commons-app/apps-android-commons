package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import fr.free.nrw.commons.R;

class ContributionsListAdapter extends CursorAdapter {
    private Activity activity;
    private ContributionController controller;
    private final int UPLOAD_BUTTON = 0;
    private final int NOTIFICATIONS = 1;

    public ContributionsListAdapter(Activity activity, Cursor c, int flags, ContributionController controller) {
        super(activity, c, flags);
        this.activity = activity;
        this.controller = controller;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView!=null) {
            /*We need to recreate views
                -if it doesnt suppose to be upload button or notification view, but attempts to use existing upload layout
                -if it supposed to be upload button or notification view, but attempts to use existing image layout
              set convertView to null so that it will be recreated in super method implementation.
             */
            if ((position != 0 && convertView.getId() == R.id.upload_grid)
                    || (position == 0 && convertView.getId() != R.id.upload_grid)
                    || (position != 1 && convertView.getId() == R.id.notification_grid)
                    || (position == 1 && convertView.getId() != R.id.notification_grid)) {
                convertView = null;
            }
        }
        return super.getView(position, convertView, parent);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        if (cursor.getPosition() == UPLOAD_BUTTON) {
            View uploadLayout = activity.getLayoutInflater().inflate(R.layout.upload_photos_grid, null, false);
            return uploadLayout;
        } else if (cursor.getPosition() == NOTIFICATIONS) {
            View notificationLayout = activity.getLayoutInflater().inflate(R.layout.notification_grid, null, false);
            return notificationLayout;
        } else {
            View parent = activity.getLayoutInflater().inflate(R.layout.layout_contribution, viewGroup, false);
            parent.setTag(new ContributionViewHolder(parent));
            return parent;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        if (cursor.getPosition() == UPLOAD_BUTTON) {

            Button galleryButton = (Button) view.findViewById(R.id.galerry_button);
            galleryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Gallery crashes before reach ShareActivity screen so must implement permissions check here
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            //See http://stackoverflow.com/questions/33169455/onrequestpermissionsresult-not-being-called-in-dialog-fragment
                            activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        } else {
                            controller.startGalleryPick();
                        }
                    } else {
                        controller.startGalleryPick();
                    }
                }
            });

            Button uploadButton = (Button) view.findViewById(R.id.upload_button);
            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controller.startCameraCapture();
                }
            });

        } else if (cursor.getPosition() == NOTIFICATIONS) {

        } else {

            final ContributionViewHolder views = (ContributionViewHolder) view.getTag();
            final Contribution contribution = Contribution.fromCursor(cursor);

            views.imageView.setMedia(contribution);
            views.titleView.setText(contribution.getDisplayTitle());

            views.seqNumView.setText(String.valueOf(cursor.getPosition() + 1));
            views.seqNumView.setVisibility(View.VISIBLE);

            switch (contribution.getState()) {
                case Contribution.STATE_COMPLETED:
                    views.stateView.setVisibility(View.GONE);
                    views.progressView.setVisibility(View.GONE);
                    views.stateView.setText("");
                    break;
                case Contribution.STATE_QUEUED:
                    views.stateView.setVisibility(View.VISIBLE);
                    views.progressView.setVisibility(View.GONE);
                    views.stateView.setText(R.string.contribution_state_queued);
                    break;
                case Contribution.STATE_IN_PROGRESS:
                    views.stateView.setVisibility(View.GONE);
                    views.progressView.setVisibility(View.VISIBLE);
                    long total = contribution.getDataLength();
                    long transferred = contribution.getTransferred();
                    if (transferred == 0 || transferred >= total) {
                        views.progressView.setIndeterminate(true);
                    } else {
                        views.progressView.setProgress((int) (((double) transferred / (double) total) * 100));
                    }
                    break;
                case Contribution.STATE_FAILED:
                    views.stateView.setVisibility(View.VISIBLE);
                    views.stateView.setText(R.string.contribution_state_failed);
                    views.progressView.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
