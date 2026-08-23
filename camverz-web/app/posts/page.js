'use client';
import { useState, useEffect } from 'react';
import { useAuth } from '../../lib/auth-context';
import { getFeedPosts, getComments, createPost, toggleLike, addComment } from '../../lib/api';
import { Heart, Trash2, Send, Clock, User, Filter, AlertCircle, MessageSquare } from 'lucide-react';
import styles from './page.module.css';

const TESTER_EMAILS = [
  'neerajjain0220@gmail.com',
  'mohitj8120@gmail.com',
  'mohitjain1619@gmail.com',
];

export default function PostsPage() {
  const { user, userData, loading: authLoading, setShowLogin, setShowAppRedirect } = useAuth();
  const [posts, setPosts] = useState([]);
  const [filter, setFilter] = useState('all'); // 'all', 'male', 'female'
  const [loading, setLoading] = useState(true);

  // iOS & Tester checks for enabling active posting
  const [isIosUser, setIsIosUser] = useState(false);
  const [isTesterUser, setIsTesterUser] = useState(false);
  const [composerText, setComposerText] = useState('');
  const [composerCategory, setComposerCategory] = useState('all');
  const [isPosting, setIsPosting] = useState(false);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const ua = navigator.userAgent || navigator.vendor || window.opera;
      setIsIosUser(/iphone|ipad|ipod/i.test(ua));
    }
  }, []);

  useEffect(() => {
    if (user?.email || userData?.email) {
      const email = user?.email || userData?.email;
      const isTester = TESTER_EMAILS.map(e => e.toLowerCase().trim()).includes(email.toLowerCase().trim());
      setIsTesterUser(isTester);
    }
  }, [user, userData]);

  const canInteractOnWeb = isIosUser || isTesterUser;

  const fetchPosts = async () => {
    if (!user) {
      setPosts([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const response = await getFeedPosts(filter);
      if (response.ok) {
        setPosts(response.posts);
      }
    } catch (err) {
      console.error('Error fetching posts:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (authLoading) return;
    fetchPosts();
  }, [filter, user, authLoading]);

  const handleCreatePost = async (e) => {
    e.preventDefault();
    if (!user) {
      setShowLogin(true);
      return;
    }
    if (!composerText.trim()) {
      alert('Please enter post text.');
      return;
    }
    setIsPosting(true);
    try {
      const res = await createPost(composerText.trim(), composerCategory);
      if (res.ok) {
        setComposerText('');
        alert('Post uploaded successfully!');
        fetchPosts(); // Reload feed
      }
    } catch (err) {
      console.error(err);
      const errMsg = err.response?.data?.error || 'Failed to upload post';
      alert(errMsg);
    } finally {
      setIsPosting(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.bgGradientPage} />
      <div className={styles.inner}>

        {/* Header */}
        <header className={styles.header}>
          <h1 className="neon-text">Community Feed</h1>
          <p>Share moments that vanish after 7 minutes. Keep it raw, keep it real.</p>
        </header>

        <div className={styles.mainLayout}>

          {/* Feed Left: Posts list and Filters */}
          <div className={styles.feedSection}>

            {/* Feed Filters */}
            <div className={styles.filtersContainer}>
              <div className={styles.filterTitle}>
                <Filter size={16} />
                <span>Show Posts:</span>
              </div>
              <div className={styles.filterButtons}>
                <button
                  className={`${styles.filterBtn} ${filter === 'all' ? styles.active : ''}`}
                  onClick={() => setFilter('all')}
                >
                  🌐 Everyone
                </button>
                <button
                  className={`${styles.filterBtn} ${filter === 'male' ? styles.active : ''}`}
                  onClick={() => setFilter('male')}
                  disabled={userData && userData.gender !== 'male'}
                  title={userData && userData.gender !== 'male' ? 'Only males can view male feeds' : ''}
                >
                  👨 Males Only
                </button>
                <button
                  className={`${styles.filterBtn} ${filter === 'female' ? styles.active : ''}`}
                  onClick={() => setFilter('female')}
                  disabled={userData && userData.gender !== 'female'}
                  title={userData && userData.gender !== 'female' ? 'Only females can view female feeds' : ''}
                >
                  👩 Females Only
                </button>
              </div>
            </div>

            {/* Posts Feed */}
            {loading ? (
              <div className={styles.loadingContainer}>
                <div className={styles.spinner} />
                <p>Loading community posts...</p>
              </div>
            ) : !user ? (
              <div className={styles.loginRequiredFeed}>
                <div className={styles.lockIcon}>🔒</div>
                <h3>Authentication Required</h3>
                <p>Sign in to view the active posts and share your moments with the community.</p>
                <button type="button" className="btn-neon" onClick={() => setShowLogin(true)} style={{ marginTop: '16px' }}>
                  Sign In with Google
                </button>
              </div>
            ) : posts.length === 0 ? (
              <div className={styles.emptyFeed}>
                <div className={styles.emptyIcon}>✍️</div>
                <h3>No active posts right now</h3>
                <p>Be the first to post something in this category!</p>
              </div>
            ) : (
              <div className={styles.postsList}>
                {posts.map((post) => (
                  <PostCard key={post.id} post={post} currentUser={user} canInteractOnWeb={canInteractOnWeb} />
                ))}
              </div>
            )}
          </div>

          {/* Feed Right: Active Create Post Composer or Mock Composer */}
          <div className={styles.composerSection}>
            {canInteractOnWeb ? (
              <form className={styles.composerCard} onSubmit={handleCreatePost}>
                <h3>Share a Moment</h3>
                <div className={styles.mockComposer} style={{ cursor: 'default' }}>
                  <div className={styles.userBar}>
                    <img
                      src={userData?.avatar ? `/avatars/${userData.avatar}.png` : '/avatars/av1.png'}
                      alt="Avatar"
                      className={styles.userAvatar}
                    />
                    <div>
                      <span className={styles.userName}>{userData?.name || 'Guest User'}</span>
                      <span className={styles.userTag}>@{userData?.customId || 'id'}</span>
                    </div>
                  </div>

                  <textarea
                    className={styles.composerTextarea}
                    placeholder="What is on your mind? Write your fantasy or thoughts..."
                    value={composerText}
                    onChange={(e) => setComposerText(e.target.value)}
                    disabled={isPosting}
                    style={{ background: 'rgba(0, 0, 0, 0.2)', border: '1px solid rgba(255, 255, 255, 0.1)', cursor: 'text' }}
                  />

                  {/* Category Selection Dropdown */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', margin: '10px 0' }}>
                    <label style={{ fontSize: '12px', color: 'rgba(255, 255, 255, 0.6)' }}>Select Category:</label>
                    <select
                      value={composerCategory}
                      onChange={(e) => setComposerCategory(e.target.value)}
                      disabled={isPosting}
                      style={{
                        background: '#1a1d24',
                        color: '#fff',
                        border: '1px solid rgba(255, 255, 255, 0.1)',
                        borderRadius: '6px',
                        padding: '8px',
                        fontSize: '14px',
                        outline: 'none'
                      }}
                    >
                      <option value="all">🌐 Everyone (All)</option>
                      {(!userData || userData.gender === 'male') && <option value="male">👨 Male</option>}
                      {(!userData || userData.gender === 'female') && <option value="female">👩 Female</option>}
                    </select>
                  </div>

                  <button type="submit" className="btn-neon" style={{ width: '100%' }} disabled={isPosting}>
                    {isPosting ? 'Posting...' : <><Send size={16} /> Post Now</>}
                  </button>
                </div>
              </form>
            ) : (
              <div className={styles.composerCard} onClick={() => setShowAppRedirect('posts')} style={{ cursor: 'pointer' }}>
                <h3>Share a Moment</h3>
                <div className={styles.mockComposer}>
                  <div className={styles.userBar}>
                    <img
                      src={userData?.avatar ? `/avatars/${userData.avatar}.png` : '/avatars/av1.png'}
                      alt="Avatar"
                      className={styles.userAvatar}
                    />
                    <div>
                      <span className={styles.userName}>{userData?.name || 'Guest User'}</span>
                      <span className={styles.userTag}>@{userData?.customId || 'id'}</span>
                    </div>
                  </div>
                  <textarea
                    className={styles.composerTextarea}
                    placeholder="What is on your mind? Write your fantasy or thoughts, and get direct messages from people..."
                    readOnly
                  />
                  <button className="btn-neon" style={{ width: '100%' }}>
                    <Send size={16} /> Post Now
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ── SUB-COMPONENT: POST CARD WITH COUNTDOWN TIMER & COMMENTS ──
function PostCard({ post, currentUser, canInteractOnWeb }) {
  const { setShowLogin, setShowAppRedirect } = useAuth();
  const [timeLeft, setTimeLeft] = useState('');
  const [showComments, setShowComments] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);

  // Like & Comment state
  const [liked, setLiked] = useState(post.likedByMe);
  const [likesCount, setLikesCount] = useState(post.likeCount || 0);
  const [isLiking, setIsLiking] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [isCommenting, setIsCommenting] = useState(false);

  useEffect(() => {
    const updateTimer = () => {
      if (!post.expiryAt) {
        setTimeLeft('Expired');
        return;
      }
      const expiry = new Date(post.expiryAt).getTime();
      const diff = expiry - Date.now();
      if (diff <= 0) {
        setTimeLeft('Expired');
        return;
      }

      const hrs = Math.floor(diff / (1000 * 60 * 60));
      const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const secs = Math.floor((diff % (1000 * 60)) / 1000);

      if (hrs > 0) {
        setTimeLeft(`${hrs}h ${mins}m`);
      } else {
        setTimeLeft(`${mins}m ${secs}s`);
      }
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [post.expiryAt]);

  const fetchComments = async () => {
    setCommentsLoading(true);
    try {
      const response = await getComments(post.id);
      if (response.ok) {
        setComments(response.comments);
      }
    } catch (err) {
      console.error('Error fetching comments:', err);
    } finally {
      setCommentsLoading(false);
    }
  };

  useEffect(() => {
    if (!showComments) return;
    fetchComments();
  }, [showComments, post.id]);

  const handleLikeClick = async () => {
    if (!currentUser) {
      setShowLogin(true);
      return;
    }
    if (!canInteractOnWeb) {
      setShowAppRedirect('posts');
      return;
    }
    if (isLiking) return;
    setIsLiking(true);
    try {
      const res = await toggleLike(post.id);
      if (res.ok) {
        setLiked(res.liked);
        setLikesCount(prev => res.liked ? prev + 1 : Math.max(0, prev - 1));
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLiking(false);
    }
  };

  const handleSendComment = async (e) => {
    if (e) e.preventDefault();
    if (!currentUser) {
      setShowLogin(true);
      return;
    }
    if (!canInteractOnWeb) {
      setShowAppRedirect('posts');
      return;
    }
    if (!commentText.trim() || isCommenting) return;
    setIsCommenting(true);
    try {
      const res = await addComment(post.id, commentText.trim());
      if (res.ok) {
        setCommentText('');
        fetchComments(); // Reload comments
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsCommenting(false);
    }
  };

  const avatarSrc = post.userAvatar
    ? `/avatars/${post.userAvatar}.png`
    : (post.userPhotoUrl || '/avatars/av1.png');

  return (
    <div className={styles.postCard}>
      {/* Top Header */}
      <div className={styles.cardHeader}>
        <div className={styles.userInfo}>
          <img src={avatarSrc} alt={post.username} className={styles.userAvatar} />
          <div>
            <span className={styles.username}>
              {post.username}
              {post.gender?.toLowerCase()?.startsWith('f') ? ' ♀️' : ' ♂️'}
              {post.verified && (
                <svg viewBox="0 0 24 24" fill="#00D9FF" width="14" height="14" style={{ display: 'inline-block', verticalAlign: 'middle', marginLeft: '4px', marginRight: '4px' }}>
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10,-4.48 10,-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                </svg>
              )}
              {post.sexPreference && (
                <span className={styles.prefBadge}>{post.sexPreference}</span>
              )}
            </span>
            <span className={styles.postTime}>
              {new Date(post.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          </div>
        </div>
        <div className={styles.timerBadge}>
          <Clock size={12} />
          <span>{timeLeft}</span>
        </div>
      </div>

      {/* Content */}
      <div className={styles.cardBody}>
        {post.text && <p className={styles.postText}>{post.text}</p>}
      </div>

      {/* Bottom Footer Actions */}
      <div className={styles.cardFooter}>
        <button
          className={styles.actionBtn}
          onClick={handleLikeClick}
          disabled={isLiking}
        >
          <Heart size={16} fill={liked ? 'var(--neon-pink)' : 'none'} stroke={liked ? 'var(--neon-pink)' : 'currentColor'} />
          <span style={{ color: liked ? 'var(--neon-pink)' : 'inherit' }}>
            {likesCount} {likesCount === 1 ? 'Like' : 'Likes'}
          </span>
        </button>

        <button
          className={`${styles.actionBtn} ${showComments ? styles.activeComments : ''}`}
          onClick={() => setShowComments(!showComments)}
        >
          <MessageSquare size={16} />
          <span>{post.commentCount || 0} Comments</span>
        </button>
      </div>

      {/* Comments Section */}
      {showComments && (
        <div className={styles.commentsSection}>
          <form className={styles.mockCommentInputContainer} onSubmit={handleSendComment}>
            <input
              type="text"
              placeholder="Write a comment... (Tell your fantasy, people will DM you!)"
              className={styles.mockCommentInput}
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              disabled={isCommenting}
              style={{ cursor: 'text' }}
            />
            <button type="submit" className={styles.mockCommentSendBtn} disabled={isCommenting}>
              <Send size={14} />
            </button>
          </form>

          {commentsLoading ? (
            <div className={styles.commentsLoading}>
              <div className={styles.commentsSpinner} />
            </div>
          ) : comments.length === 0 ? (
            <div className={styles.noComments}>No comments yet. Be the first to comment!</div>
          ) : (
            <div className={styles.commentsList}>
              {comments.map((comment) => {
                const commentAvatarSrc = comment.userAvatar
                  ? `/avatars/${comment.userAvatar}.png`
                  : (comment.userPhotoUrl || '/avatars/av1.png');

                return (
                  <div key={comment.id} className={styles.commentItem}>
                    <img
                      src={commentAvatarSrc}
                      alt={comment.username}
                      className={styles.commentAvatar}
                    />
                    <div className={styles.commentContent}>
                      <div className={styles.commentUserHeader}>
                        <span className={styles.commentUsername}>{comment.username}</span>
                        <span className={styles.commentTime}>
                          {new Date(comment.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <p className={styles.commentText}>{comment.text}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
