# StackView
一个自定义可以自动轮播的层叠View


![](https://github.com/nbwzlyd/StackView/blob/master/StackView/app/gif/loop.gif)
照例，先上图片看效果

github上倒是有不少类似的效果库，不过对于这个功能来说，多少有点写的复杂，我们没必要因为这个功能区down一个库下来，所以，自己动手，丰衣足食。仿写的Adapter模式~

## ViewGroup的选择 ##

自定义ViewGroup无外乎两个重要的方法，**onMeasure()**和**onLayout();**
至于其中含义，可参考网上资料。
看过github上的一些库，大部分是继承自ViewGroup，或者是自定义Layoutmangaer来实现，当然这些实现的功能是可拖拽的层叠View，我们并不需要这么复杂。但是我们也不会继承自ViewGroup，原因如下：
1.继承自ViewGroup代价太大，有点舍近求远的意思，我们需要再onMeasure(),onLayout()中处理大量逻辑，稍微疏忽可能出现一些意想不到的错误。
2.观察效果可以发现，既然是层叠View，一张叠在另一张上，假设不自动轮播，只是实现这个效果，只需要在xml中写，FrameLayout就可以实现。所以思路就有了，我们可以直接继承FrameLayout，在其中实现逻辑即可。
3.官方的FrameLayout在**onMeasure()**和**onLayout()**中已经实现了初步的测量与布局操作，妥妥的，我们无需再去测量子View的宽高，方便。
其次，OnLayout函数中也处理了一些特殊的情况，例如margin等，所以我们也可以不用去管一些margin或者gravity啥的，官方已经处理，我们只需要实现层叠即可。
4.看FrameLayout的源码，这是最简单的ViewGroup实现类，性能上没大问题，假设我们自己也继承ViewGroup去实现，逻辑基本相似。所以我们为啥造车呢，拿来用即可。
5. 如果拖拽，建议继承layoutManger实现

##  onMeasure() 只针对wrap_content 情况##
如上文所述，由于我们是继承的FrameLayout，所以我们并不需要在onMeasure() 中进行子View的测量工作，但是我们需要重新定义父View的宽高，原因就是我们现在视觉上可以看到三张图片，但是父View的高度只有一张卡片的高度，所以需要重新定义父View的高度，否则，子view将会被遮挡（在wrap_content）情况下。
代码如下：

```
@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //重新定义父View的高度，否则会导致子View被遮挡;这里并没有区分mode的类型，按道理是只需要处理wrap_content
        float parentHeight;
        float scaleHeight = 0;
        int childHeight = 0;
        int childWidth = 0;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View childView = getChildAt(i);
            if (childView != null) {
                scaleHeight += childView.getTranslationY() - (childView.getTranslationY() * childView.getScaleY());
                if (i == getChildCount() - 1) {//只需测量第一个高度即可
                    childHeight = childView.getMeasuredHeight();
                    childWidth =childView.getMeasuredWidth();
                }
            }
        }
        parentHeight = childHeight + scaleHeight;
        setMeasuredDimension(childWidth , (int) parentHeight);
        Log.d(TAG, "onMeasure: " + parentHeight + " (int) parentHeight)==" + (int) parentHeight);

    }
```

其中要注意的是循环逻辑 **for (int i = getChildCount() - 1; i >= 0; i--)**  因为视觉上第一个是ViewGroup里的最后一个，故从getChildCount() - 1开始，事实上从0 开始也行，因为每个View的大小是一样的，为了逻辑统一，从视觉第一个测量 。
另外，父View的高度=一张卡片的高度+偏移量Translation_Y，但是因为有缩放效果，所以 Translation_Y的值需要打折扣 ***scaleHeight=childView.getTranslationY() - (childView.getTranslationY() * childView.getScaleY());***
所以父布局的总高度就是 ***parentHeight = childHeight + scaleHeight;***
所以第一步View的测量结束。

接下来是代码的核心部分，OnLayout阶段。

## OnLayout##
确定子view的位置
观察效果可以知道，每一张卡片之间都会有一定的偏移量与缩放量，我们暂且定一个配置类（可后续拓展为可配置）

```
 public class CardConfig {
        public int MAX_VISIBLE_COUNT = 3;
        public int BASE_TRANSLATION_Y = dip2px(mContext, 20);
        public float BASE_SCALE = 0.08F;
    }
```
核心代码如下
```
@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getChildCount() == 0) {
            initView();
        }
    }

    //初始化布局
    public void initView() {
        int itemCount = mAdapter.getCount();

        int layoutCount = Math.min(itemCount, CardConfig.MAX_VISIBLE_COUNT);
        int viewLevel;//View的层级
        boolean flag = layoutCount > 1;

        for (int pos = 0; flag ? pos <= layoutCount : pos < layoutCount; pos++) {//比最大可见数量多布局一个，动画更顺畅
            mStartIndex = pos % itemCount;
            View childView = mAdapter.getView(mStartIndex, null, this);
            viewLevel = getChildCount();
            if (pos == layoutCount) {
                viewLevel = pos - 1;
            }
            childView.setTranslationY(viewLevel * CardConfig.BASE_TRANSLATION_Y);
            childView.setScaleX(1 - viewLevel * CardConfig.BASE_SCALE);
            childView.setScaleY(1 - viewLevel * CardConfig.BASE_SCALE);
            addViewInLayout(childView, 0, childView.getLayoutParams());
            childView.requestLayout();
        }
        Log.d(TAG, "onLayout:" + getChildCount());
    }
```

1.视觉上虽然只能看到三个卡片，但是我们要预加载一个，这样动画看起来会更加流畅些，否则会出现，第一个消失后，第三个卡片突然很生硬的出现，当然你可以做动画，但是这样明显不是很好的一个方案。

**2.视觉层级，如何让源数据里的第一个图片永远显示在最上面？**
如果我们直接addView或者addViewInLayout的话，第一个添加进ViewGroup里的子View会被后续增加的View覆盖，这显然不是产品经理要的效果，所以我们需要指定View的层级，用addViewInLayout方法，并指定参数index 为0 ，这样后续添加进去的View就都再最下面了。
为啥用addViewInLayout 而不用addView呢，看源码可知道，addView方法每次会调用requestLayout方法，对于现有功能来说，这个是性能的浪费，没必要，所以addViewInLayout首选，参考链接 [What is the difference between addView and addViewInLayout？](https://blog.csdn.net/u011045817/article/details/50343335)。

**3.View层级**
没啥可说的，一共三层，4个view ，第四个层级与第三个层级一样，故 
```
viewLevel = getChildCount();
if (pos == layoutCount) {
     viewLevel = pos - 1;
 }
```
## 动画 ##
动画效果很简单，观察可知道，第一个View是没有缩放变化的，下面的View由小变大，是规律的，比如第三个View的大小变为第二个大小，最后一个View不动，以此类推。所以只需要特殊处理一下第一个View和最后一个即可

```
 public void exitWithAnimation() {
        for (int index = getChildCount() - 1; index >= 0; index--) {
            View childView = getChildAt(index);
            if (index == getChildCount() - 1) {//第一个view
                ObjectAnimator alpha = ObjectAnimator.ofFloat(childView, "alpha", 1f, 0f);
                alpha.setDuration(1000);
                alpha.start();
                alpha.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                        View deleteView = getChildAt(getChildCount() - 1);
                        mCacheView.put(R.id.imageView, deleteView);//讲移除掉的view进行缓存，避免每次创建,
                        // 由于每个View的类型是一样的，所以以同一个id为key即可，否则将根据viewType类型为key进行缓存
                        removeViewInLayout(deleteView);
                        makeView();
                        mHandler.postDelayed(mRunnable, 1000);//循环

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
            } else if (index == 0) {//最后一个view不动

            } else {

                View lastView = getChildAt(index + 1);

                ObjectAnimator scaleX = ObjectAnimator.ofFloat(childView, "scaleX", childView.getScaleX(), lastView.getScaleX());
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(childView, "scaleY", childView.getScaleY(), lastView.getScaleY());
                ObjectAnimator translateY = ObjectAnimator.ofFloat(childView, "translationY", childView.getTranslationY(), lastView.getTranslationY());
                AnimatorSet set = new AnimatorSet();
                set.play(scaleX).with(scaleY).with(translateY);
                set.setDuration(1000);
                set.start();
            }
        }
    }
```

**关键点** 1.每次第一个View的动画执行完毕后，我们需要将其移除。并添加新的View到最底层，实现循环。
				2. 需要将移除的View进行缓存，我们没必要每次都去创建新的图片，这是性能的浪费，由于我们的View类型都是一样的，所以任意定义个key即可，否则可根据ViewType类型来特殊处理与缓存
		

```
mCacheView.put(R.id.imageView, deleteView);//讲移除掉的view进行缓存，避免每次创建,
```
3 ，创建新的View添加至队尾
 

```
private void makeView() {
        mStartIndex++;
        if (mStartIndex == mAdapter.getCount()) {
            mStartIndex = 0;
        }
        View covertView = mCacheView.get(R.id.imageView);//缓存的消失的view
        mAdapter.getView(mStartIndex, covertView, this);
        covertView.setAlpha(1);//恢复状态，否则alpha还是0;
        int level = getChildCount() - 1;
        covertView.setTranslationY(level * CardConfig.BASE_TRANSLATION_Y);
        covertView.setScaleX(1 - (level * CardConfig.BASE_SCALE));
        covertView.setScaleY(1 - (level * CardConfig.BASE_SCALE));
        addView(covertView, 0, covertView.getLayoutParams());
        Log.d(TAG, "makeView: " + getChildCount());
    }
```
先取缓存的View，如果为空则创建新的，否则就用缓存的，同时要mStartIndex++，调用mAdapter.getView(mStartIndex, covertView, this);进行数据的刷新操作，要注意记得将缓存的View的alpha设置为1，因为缓存的View的alpha都是0，（动画效果，）故要恢复初始状态。

其他要注意的事项就是在View被移除屏幕时记得销毁动画，否则一直post.......

```
@Override
    protected void onDetachedFromWindow() {//view从屏幕移除时候及时销毁动画
        super.onDetachedFromWindow();
        mRunnable = null;
        mHandler.removeCallbacks(null);
        mIsAnimation = false;
    }
```

其他逻辑参考github链接吧，代码写的很简单了~~
https://github.com/nbwzlyd/StackView
