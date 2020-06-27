# ExoPlayerTimelineView

If you already use ExoPlayer in your project and need to extract video
frames and show them as a **timeline view** either scrollable or in a
fixed-width mode then you're in the right place.

<p align="center">
<img src="screens/1_shot.jpg" width="220">
</p>

### Installation
Add this to your application module, inside *dependencies* block.
```sh
dependencies {
    implementation 'com.chemicalbird.android:videotimelineview:0.0.1'
}
```

### Usage example

1. Add timeline View to your layout

```sh
<com.video.timeline.ScrollableTimelineGlView
        android:id="@+id/thumb_list"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="22dp"/>
```

2. To create the frame generator pass video **uri** and set an
   implementation of the image(frame) loader (Picasso, Glide, ..whatever
   you are using in your project)
```sh
ScrollableTimelineGlView scrollableTimelineGlView = findViewById(R.id.thumb_list);
VideoTimeLine videoTimeLine = VideoTimeLine.with(fileUri)
        .setImageLoader(picassoLoader)
        .setFrameDuration(1)
        .setFrameSizeDp(50)
        .into(scrollableTimelineGlView);

RecyclerView recyclerView = scrollableTimelineGlView.getRecyclerView();
// get underlying RecyclerView to do your own UI customizations
```

3. To show the timeline view call ```videoTimeLine.start()```

