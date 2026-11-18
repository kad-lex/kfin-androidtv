package org.jellyfin.androidtv.ui.playback.overlay;

import static java.lang.Math.round;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowView;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.ClockBehavior;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.action.AndroidAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChannelBarChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChapterAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ClosedCaptionsAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.CustomAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.FastForwardAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.GuideAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PlayPauseAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PlaybackSpeedAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PreviousLiveTvChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.RecordAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.RewindAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SelectAudioAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SelectQualityAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SkipNextAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SkipPreviousAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ZoomAction;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.koin.java.KoinJavaComponent;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class CustomPlaybackTransportControlGlue extends PlaybackTransportControlGlue<VideoPlayerAdapter> {

    // Normal playback actions
    private PlayPauseAction playPauseAction;
    private RewindAction rewindAction;
    private FastForwardAction fastForwardAction;
    private SkipPreviousAction skipPreviousAction;
    private SkipNextAction skipNextAction;
    private SelectAudioAction selectAudioAction;
    private ClosedCaptionsAction closedCaptionsAction;
    private SelectQualityAction selectQualityAction;
    private PlaybackSpeedAction playbackSpeedAction;
    private ZoomAction zoomAction;
    private ChapterAction chapterAction;

    // TV actions
    private PreviousLiveTvChannelAction previousLiveTvChannelAction;
    private ChannelBarChannelAction channelBarChannelAction;
    private GuideAction guideAction;
    private RecordAction recordAction;

    private final PlaybackController playbackController;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;

    // Injected views
    private TextView mEndsText = null;

    private final Handler mHandler = new Handler();
    private Runnable mRefreshEndTime;
    private Runnable mRefreshViewVisibility;

    private LinearLayout mButtonRef;

    CustomPlaybackTransportControlGlue(Context context, VideoPlayerAdapter playerAdapter, PlaybackController playbackController) {
        super(context, playerAdapter);
        this.playbackController = playbackController;

        mRefreshEndTime = () -> {
            setEndTime();
            if (!isPlaying()) {
                mHandler.postDelayed(mRefreshEndTime, 30000);
            }
        };

        mRefreshViewVisibility = () -> {
            if (mButtonRef != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
                mEndsText.setVisibility(mButtonRef.getVisibility());
            else
                mHandler.postDelayed(mRefreshViewVisibility, 100);
        };

        initActions(context);
    }

    @Override
    protected void onDetachedFromHost() {
        mHandler.removeCallbacks(mRefreshEndTime);
        mHandler.removeCallbacks(mRefreshViewVisibility);

        closedCaptionsAction.removePopup();
        playbackSpeedAction.dismissPopup();
        selectAudioAction.dismissPopup();
        selectQualityAction.dismissPopup();
        zoomAction.dismissPopup();

        super.onDetachedFromHost();
    }

    @Override
    protected PlaybackRowPresenter onCreateRowPresenter() {
        final AbstractDetailsDescriptionPresenter detailsPresenter = new AbstractDetailsDescriptionPresenter() {
            @Override
            protected void onBindDescription(ViewHolder vh, Object item) {

            }
        };
        PlaybackTransportRowPresenter rowPresenter = new PlaybackTransportRowPresenter() {
            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                RowPresenter.ViewHolder vh = super.createRowViewHolder(parent);

                // KFIN: matte glass panel (30%) behind the controls + title block
                if (vh.view instanceof ViewGroup) {
                    ViewGroup rowRoot = (ViewGroup) vh.view;
                    if (rowRoot.getChildCount() > 1) {
                        View transportRow = rowRoot.getChildAt(1);
                        android.graphics.drawable.GradientDrawable panel = new android.graphics.drawable.GradientDrawable();
                        panel.setCornerRadius(dp(parent.getContext(), 24));
                        panel.setColor(0x99000000); // KFIN: 60% black glass
                        panel.setStroke(dp(parent.getContext(), 1), 0x24FFFFFF);
                        transportRow.setBackground(panel);
                        // KFIN: panel spans nearly the full width so every button fits
                        transportRow.setPadding(
                                dp(parent.getContext(), 8), dp(parent.getContext(), 8),
                                dp(parent.getContext(), 8), dp(parent.getContext(), 8));
                    }
                    // KFIN: center the primary control bar inside its dock
                    // (the dock is match_parent, so the bar itself needs center gravity)
                    View primaryDock = vh.view.findViewById(androidx.leanback.R.id.controls_dock);
                    if (primaryDock instanceof FrameLayout && ((FrameLayout) primaryDock).getChildCount() > 0) {
                        View bar = ((FrameLayout) primaryDock).getChildAt(0);
                        if (bar.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) bar.getLayoutParams();
                            flp.gravity = Gravity.CENTER;
                            bar.setLayoutParams(flp);
                        }
                    }
                    // KFIN: duration indicator bold, two sp larger, 80% white
                    int[] timeIds = new int[]{androidx.leanback.R.id.current_time, androidx.leanback.R.id.total_time, androidx.leanback.R.id.separate_time};
                    for (int timeId : timeIds) {
                        TextView timeView = vh.view.findViewById(timeId);
                        if (timeView != null) {
                            float sp = timeView.getTextSize() / parent.getContext().getResources().getDisplayMetrics().scaledDensity;
                            // KFIN: timer at double size, vertically centered with the buttons
                            timeView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp * 2);
                            timeView.setTypeface(null, android.graphics.Typeface.BOLD);
                            timeView.setTextColor(0xCCFFFFFF);
                            if (timeView.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                                RelativeLayout.LayoutParams tlp = (RelativeLayout.LayoutParams) timeView.getLayoutParams();
                                tlp.addRule(RelativeLayout.CENTER_VERTICAL);
                                tlp.topMargin = 0;
                                timeView.setLayoutParams(tlp);
                            }
                        }
                    }
                    // KFIN: the whole panel sits at the screen's bottom edge,
                    // 10px up - menus and the logo follow its position
                    vh.view.post(() -> {
                        if (rowRoot.getChildCount() < 2) return;
                        View panel = rowRoot.getChildAt(1);
                        int[] loc = new int[2];
                        panel.getLocationOnScreen(loc);
                        int bottom = loc[1] + panel.getHeight();
                        int screenH = panel.getResources().getDisplayMetrics().heightPixels;
                        int delta = screenH - dp(parent.getContext(), 10) - bottom;
                        if (delta > 0) panel.setY(panel.getY() + delta);
                    });

                    // KFIN: progress bar at 60% opacity
                    View progress = vh.view.findViewById(androidx.leanback.R.id.playback_progress);
                    if (progress != null) progress.setAlpha(0.6f);

                    // KFIN: detach the time row (current/separator/total) from the
                    // button row and re-insert it directly above the seekbar;
                    // the secondary dock gets its own full-width slot below
                    View totalTimeView = vh.view.findViewById(androidx.leanback.R.id.total_time);
                    View dockView = vh.view.findViewById(androidx.leanback.R.id.secondary_controls_dock);
                    if (progress != null && totalTimeView != null && dockView != null
                            && progress.getParent() instanceof ViewGroup
                            && totalTimeView.getParent() instanceof ViewGroup
                            && dockView.getParent() == totalTimeView.getParent()) {
                        ViewGroup transport = (ViewGroup) progress.getParent();
                        ViewGroup timeRow = (ViewGroup) totalTimeView.getParent();
                        timeRow.removeView(dockView);
                        transport.removeView(timeRow);
                        int progressIndex = transport.indexOfChild(progress);
                        FrameLayout dockSlot = new FrameLayout(parent.getContext());
                        dockSlot.addView(dockView, new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        transport.addView(dockSlot, progressIndex + 1);

                        // KFIN: bottom bar centered, same as the primary row
                        for (int i = 0; i < ((ViewGroup) dockView).getChildCount(); i++) {
                            View child = ((ViewGroup) dockView).getChildAt(i);
                            if (child.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                                FrameLayout.LayoutParams clp = (FrameLayout.LayoutParams) child.getLayoutParams();
                                clp.gravity = Gravity.CENTER_HORIZONTAL;
                                child.setLayoutParams(clp);
                            }
                        }

                        // KFIN: the time row shares the primary dock with the
                        // rewind/play/ff buttons - a single floor above the seekbar
                        View primaryDockView = vh.view.findViewById(androidx.leanback.R.id.controls_dock);
                        if (primaryDockView instanceof ViewGroup) {
                            transport.removeView(timeRow);
                            FrameLayout.LayoutParams trlp = new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                                    Gravity.CENTER_VERTICAL);
                            ((ViewGroup) primaryDockView).addView(timeRow, trlp);
                        }

                        // KFIN: the title moves onto the time row (left side),
                        // without its backdrop - after attach so the root view
                        // actually contains the top panel
                        vh.view.post(() -> {
                            int[] titleIds = new int[]{R.id.item_title, R.id.item_subtitle};
                            for (int titleId : titleIds) {
                                View titleView = vh.view.getRootView().findViewById(titleId);
                                if (titleView != null && titleView.getParent() != timeRow && !titleView.hasFocus()) {
                                    titleView.setBackground(null);
                                    if (titleView.getParent() instanceof ViewGroup) {
                                        ((ViewGroup) titleView.getParent()).removeView(titleView);
                                    }
                                    RelativeLayout.LayoutParams tlp = new RelativeLayout.LayoutParams(
                                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    tlp.addRule(RelativeLayout.ALIGN_PARENT_START);
                                    // KFIN: two lines, no overlap - the subtitle
                                    // sits directly below the title
                                    if (titleId == R.id.item_subtitle) {
                                        tlp.addRule(RelativeLayout.BELOW, R.id.item_title);
                                        View titleText = vh.view.getRootView().findViewById(R.id.item_title);
                                        if (titleText != null) {
                                            titleView.setPadding(titleText.getPaddingLeft(), titleView.getPaddingTop(),
                                                    titleView.getPaddingRight(), titleView.getPaddingBottom());
                                        }
                                    }
                                    timeRow.addView(titleView, 0, tlp);
                                }
                            }
                        });
                    }

                }

                // KFIN: center the secondary (bottom) controls row horizontally
                View secondaryDock = vh.view.findViewById(androidx.leanback.R.id.secondary_controls_dock);
                if (secondaryDock != null && secondaryDock.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams slp = (RelativeLayout.LayoutParams) secondaryDock.getLayoutParams();
                    slp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    // KFIN: the button bar spans the full panel width so all
                    // actions fit; the time labels no longer share its row
                    slp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    slp.addRule(RelativeLayout.ALIGN_PARENT_START);
                    slp.addRule(RelativeLayout.ALIGN_PARENT_END);
                    secondaryDock.setLayoutParams(slp);
                }

                ClockBehavior showClock = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getClockBehavior());

                if (showClock == ClockBehavior.ALWAYS || showClock == ClockBehavior.IN_VIDEO) {
                    Context context = parent.getContext();
                    mEndsText = new TextView(context);
                    mEndsText.setTextAppearance(context, androidx.leanback.R.style.Widget_Leanback_PlaybackControlsTimeStyle);
                    setEndTime();

                    LinearLayout view = (LinearLayout) vh.view;

                    PlaybackTransportRowView bar = (PlaybackTransportRowView) view.getChildAt(1);
                    FrameLayout v = (FrameLayout) bar.getChildAt(0);
                    mButtonRef = (LinearLayout) v.getChildAt(0);

                    bar.removeViewAt(0);
                    RelativeLayout rl = new RelativeLayout(context);
                    RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    rl.addView(v);

                    RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    rlp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    rlp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    rl.addView(mEndsText, rlp2);
                    bar.addView(rl, 0, rlp);
                }

                return vh;
            }

            @Override
            protected void onProgressBarClicked(PlaybackTransportRowPresenter.ViewHolder vh) {
                CustomPlaybackTransportControlGlue controlglue = CustomPlaybackTransportControlGlue.this;
                controlglue.onActionClicked(controlglue.playPauseAction);
            }

            @Override
            protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
                super.onBindRowViewHolder(vh, item);
                vh.setOnKeyListener(CustomPlaybackTransportControlGlue.this);
                installTransportStyler(vh.view);
            }

            @Override
            protected void onRowViewAttachedToWindow(RowPresenter.ViewHolder vh) {
                super.onRowViewAttachedToWindow(vh);
                // KFIN: action views can be created after the first bind - restyle on attach
                installTransportStyler(vh.view);
            }

            @Override
            protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
                super.onUnbindRowViewHolder(vh);
                vh.setOnKeyListener(null);
            }
        };
        rowPresenter.setDescriptionPresenter(detailsPresenter);
        return rowPresenter;
    }

    // leanback stores its ViewHolder in the row view's tag - never touch it;
    // keep our own weak set of roots that already have the layout listener
    private static final java.util.Set<View> STYLED_ROOTS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    // KFIN: leanback creates/replaces the control views at arbitrary times, so
    // restyle on every layout pass instead of relying on one-shot posts
    private static void installTransportStyler(View root) {
        root.post(() -> styleTransportButtons(root));
        if (!STYLED_ROOTS.add(root)) return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> styleTransportButtons(root));
    }
    private static final int KFIN_BTN_BG = 0x99000000; // KFIN: 60% black
    private static final int KFIN_BTN_STROKE = 0x24FFFFFF;
    private static final int KFIN_BTN_FOCUS_BG = 0xCCFFFFFF;

    private static int dp(Context context, int value) {
        return round(value * context.getResources().getDisplayMetrics().density);
    }

    private static android.graphics.drawable.Drawable createTransportButtonBackground(boolean focused, Context context) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        d.setCornerRadius(999f);
        d.setColor(focused ? KFIN_BTN_FOCUS_BG : KFIN_BTN_BG);
        // KFIN: rest state - white 2px outline at 80% (same style family as the focus ring)
        d.setStroke(dp(context, 1), focused ? KFIN_BTN_STROKE : 0xCCFFFFFF);
        return d;
    }

    private static boolean isControlBarChild(View view) {
        if (view.getId() != androidx.leanback.R.id.button && view.getId() != androidx.leanback.R.id.icon) {
            return false;
        }
        // KFIN: the button/icon ImageViews sit inside a per-action FrameLayout -
        // walk up to the ControlBar instead of checking the direct parent
        android.view.ViewParent parent = view.getParent();
        while (parent instanceof View) {
            if (parent.getClass().getName().contains("ControlBar")) return true;
            parent = parent.getParent();
        }
        return false;
    }

    private static void styleTransportButtons(View view) {
        if (view instanceof ImageView && isControlBarChild(view)) {
            ImageView iv = (ImageView) view;
            boolean isIcon = view.getId() == androidx.leanback.R.id.icon;
            // KFIN: no square focus highlight sticking out of the pill - the
            // action root also carries a selectable foreground/background
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                view.setDefaultFocusHighlightEnabled(false);
                if (view.getParent() instanceof View) {
                    View actionRoot = (View) view.getParent();
                    actionRoot.setDefaultFocusHighlightEnabled(false);
                    actionRoot.setForeground(null);
                    actionRoot.setBackground(null);
                }
            }
            // KFIN: invert the icon whenever anything in its focus chain is
            // focused (the bg ImageView, the action root or the icon itself);
            // recomputed on every layout pass - never tint the pill itself
            if (isIcon && view.getParent() instanceof View) {
                // KFIN: the pill's background (id=button) is what actually gets
                // focus and turns white - invert the icon from ITS focus state
                final View actionRoot = (View) iv.getParent();
                final View bg = actionRoot.findViewById(androidx.leanback.R.id.button);
                final Runnable invert = () -> iv.setColorFilter(
                        (actionRoot.isFocused() || (bg != null && bg.isFocused()) || iv.isFocused())
                                ? 0xE6000000 : 0xD9FFFFFF);
                invert.run();
                actionRoot.setOnFocusChangeListener((v, hasFocus) -> invert.run());
                if (bg != null) bg.setOnFocusChangeListener((v, hasFocus) -> invert.run());
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) styleTransportButtons(g.getChildAt(i));
        }
    }

    private void initActions(Context context) {
        playPauseAction = new PlayPauseAction(context);
        rewindAction = new RewindAction(context);
        fastForwardAction = new FastForwardAction(context);
        skipPreviousAction = new SkipPreviousAction(context);
        skipNextAction = new SkipNextAction(context);
        selectAudioAction = new SelectAudioAction(context, this);
        selectAudioAction.setLabels(new String[]{context.getString(R.string.lbl_audio_track)});
        closedCaptionsAction = new ClosedCaptionsAction(context, this);
        closedCaptionsAction.setLabels(new String[]{context.getString(R.string.lbl_subtitle_track)});
        selectQualityAction = new SelectQualityAction(context, this, KoinJavaComponent.get(UserPreferences.class));
        selectQualityAction.setLabels(new String[]{context.getString(R.string.lbl_quality_profile)});
        playbackSpeedAction = new PlaybackSpeedAction(context, this, playbackController);
        playbackSpeedAction.setLabels(new String[]{context.getString(R.string.lbl_playback_speed)});
        zoomAction = new ZoomAction(context, this);
        zoomAction.setLabels(new String[]{context.getString(R.string.lbl_zoom)});
        chapterAction = new ChapterAction(context, this);
        chapterAction.setLabels(new String[]{context.getString(R.string.lbl_chapters)});

        previousLiveTvChannelAction = new PreviousLiveTvChannelAction(context, this);
        previousLiveTvChannelAction.setLabels(new String[]{context.getString(R.string.lbl_prev_item)});
        channelBarChannelAction = new ChannelBarChannelAction(context, this);
        channelBarChannelAction.setLabels(new String[]{context.getString(R.string.lbl_other_channels)});
        guideAction = new GuideAction(context, this);
        guideAction.setLabels(new String[]{context.getString(R.string.lbl_live_tv_guide)});
        recordAction = new RecordAction(context, this);
        recordAction.setLabels(new String[]{
                context.getString(R.string.lbl_record),
                context.getString(R.string.lbl_cancel_recording)
        });
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        this.primaryActionsAdapter = primaryActionsAdapter;
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        this.secondaryActionsAdapter = secondaryActionsAdapter;
    }

    void addMediaActions() {
        if (primaryActionsAdapter.size() > 0)
            primaryActionsAdapter.clear();
        if (secondaryActionsAdapter.size() > 0)
            secondaryActionsAdapter.clear();

        // KFIN: rewind/play/fast-forward live in the primary row above the
        // seekbar; everything else stays in the bottom row
        VideoPlayerAdapter playerAdapter = getPlayerAdapter();

        if (playerAdapter.canSeek()) {
            primaryActionsAdapter.add(rewindAction);
        }

        primaryActionsAdapter.add(playPauseAction);

        if (playerAdapter.canSeek()) {
            primaryActionsAdapter.add(fastForwardAction);
        }

        if (playerAdapter.isLiveTv()) {
            secondaryActionsAdapter.add(channelBarChannelAction);
            secondaryActionsAdapter.add(guideAction);
        }

        if (playerAdapter.isLiveTv()) {
            secondaryActionsAdapter.add(previousLiveTvChannelAction);
            if (playerAdapter.canRecordLiveTv()) {
                secondaryActionsAdapter.add(recordAction);
                recordingStateChanged();
            }
        }

        if (playerAdapter.hasPreviousItem()) {
            secondaryActionsAdapter.add(skipPreviousAction);
        }

        if (playerAdapter.hasNextItem()) {
            secondaryActionsAdapter.add(skipNextAction);
        }

        if (playerAdapter.hasChapters()) {
            secondaryActionsAdapter.add(chapterAction);
        }

        if (playerAdapter.hasSubs()) {
            secondaryActionsAdapter.add(closedCaptionsAction);
        }

        if (playerAdapter.hasMultiAudio()) {
            secondaryActionsAdapter.add(selectAudioAction);
        }

        if (!playerAdapter.isLiveTv()) {
            secondaryActionsAdapter.add(playbackSpeedAction);
            secondaryActionsAdapter.add(selectQualityAction);
        }

        secondaryActionsAdapter.add(zoomAction);
    }

    @Override
    public void onActionClicked(Action action) {
        if (action instanceof AndroidAction) {
            ((AndroidAction) action).onActionClicked(getPlayerAdapter());
        }
        notifyActionChanged(action);
    }

    public void onCustomActionClicked(Action action, View view) {
        // Handle custom action clicks which require a popup menu
        if (action instanceof CustomAction) {
            ((CustomAction) action).handleClickAction(playbackController, getPlayerAdapter(), getContext(), view);
        }

        if (action == playbackSpeedAction) {
            // Post a callback to calculate the new time, since Exoplayer updates this in an async fashion.
            // This is a hack, we should instead have onPlaybackParametersChanged call out to this
            // class to notify rather than poll. But communication is unidirectional at the moment:
            mHandler.postDelayed(mRefreshEndTime, 5000);  // 5 seconds
        }
    }

    private void setEndTime() {
        if (mEndsText == null || getPlayerAdapter().getDuration() < 1)
            return;
        long msLeft = getPlayerAdapter().getDuration() - getPlayerAdapter().getCurrentPosition();
        long realTimeLeft = round(msLeft / playbackController.getPlaybackSpeed());

        LocalDateTime endTime = LocalDateTime.now().plus(realTimeLeft, ChronoUnit.MILLIS);
        mEndsText.setText(getContext().getString(R.string.lbl_playback_control_ends, DateTimeExtensionsKt.getTimeFormatter(getContext()).format(endTime)));
    }

    private void notifyActionChanged(Action action) {
        ArrayObjectAdapter adapter = primaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = secondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
        }
    }

    void setInitialPlaybackDrawable() {
        playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
        notifyActionChanged(playPauseAction);
    }

    void invalidatePlaybackControls() {
        if (primaryActionsAdapter.size() > 0)
            primaryActionsAdapter.clear();
        if (secondaryActionsAdapter.size() > 0)
            secondaryActionsAdapter.clear();
        addMediaActions();
    }

    void recordingStateChanged() {
        if (getPlayerAdapter().isRecording()) {
            recordAction.setIndex(RecordAction.INDEX_RECORDING);
        } else {
            recordAction.setIndex(RecordAction.INDEX_INACTIVE);
        }
        notifyActionChanged(recordAction);
    }

    void updatePlayState() {
        playPauseAction.setIndex(isPlaying() ? PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE : PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
        notifyActionChanged(playPauseAction);
        setEndTime();
        if (!isPlaying()) {
            mHandler.removeCallbacks(mRefreshEndTime);
            mHandler.postDelayed(mRefreshEndTime, 30000);
        } else {
            mHandler.removeCallbacks(mRefreshEndTime);
        }

    }

    public void setInjectedViewsVisibility() {
        if (mButtonRef != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
            mEndsText.setVisibility(mButtonRef.getVisibility());
        mHandler.removeCallbacks(mRefreshViewVisibility);
        mHandler.postDelayed(mRefreshViewVisibility, 100);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            // Let the playback glue handle the key before applying the focus fallback below.
            if (super.onKey(v, keyCode, event)) return true;

            // Leanback UI doesn't work within compose. We need to implement a custom focus handler
            // to work around various leanback issues on older Android platforms
            if (event.getAction() == KeyEvent.ACTION_DOWN) return moveFocus(v, keyCode);
            return false;
        }

        VideoPlayerAdapter playerAdapter = getPlayerAdapter();

        if (playerAdapter.hasSubs() && keyCode == KeyEvent.KEYCODE_CAPTIONS) {
            closedCaptionsAction.handleClickAction(playbackController, getPlayerAdapter(), getContext(), v);
        }
        if (playerAdapter.hasMultiAudio() && keyCode == KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK) {
            selectAudioAction.handleClickAction(playbackController, getPlayerAdapter(), getContext(), v);
        }
        return super.onKey(v, keyCode, event);
    }

    private boolean moveFocus(View root, int keyCode) {
        final int direction;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                direction = View.FOCUS_UP;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                direction = View.FOCUS_DOWN;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                direction = View.FOCUS_LEFT;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                direction = View.FOCUS_RIGHT;
                break;
            default:
                return false;
        }

        View focused = root.findFocus();
        if (focused == null) return false;

        View next = focused.focusSearch(direction);
        if (next != null && next != focused) next.requestFocus(direction);

        return true;
    }
}
