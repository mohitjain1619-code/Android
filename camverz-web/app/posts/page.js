'use client';
import { useState, useEffect } from 'react';
import { useAuth } from '../../lib/auth-context';
import { getFeedPosts, getComments } from '../../lib/api';
import { Heart, Trash2, Send, Clock, User, Filter, AlertCircle, MessageSquare } from 'lucide-react';
import styles from './page.module.css';

export default function PostsPage() {
  const { user, userData, loading: authLoading, setShowLogin, setShowAppRedirect } = useAuth();
  const [posts, setPosts] = useState([]);
  const [filter, setFilter] = useState('all'); // 'all', 'male', 'female'
  const [loading, setLoading] = useState(true);

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

  return (
    <div className={styles.container}>
      <div className={styles.bgGradientPage} />
      <div className={styles.inner}>
        
        {/* Header */}
        <header className={styles.header}>
          <h1 className="neon-text">Community Feed</h1>
          <p>Share moments that vanish after 2 hours. Keep it raw, keep it real.</p>
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
                  <PostCard key={post.id} post={post} currentUser={user} />
                ))}
              </div>
            )}
          </div>

          {/* Feed Right: Interactive Mock Create Post Composer (Sticky) */}
          <div className={styles.composerSection}>
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
          </div>
        </div>
      </div>
    </div>
  );
}

// ── SUB-COMPONENT: POST CARD WITH COUNTDOWN TIMER & COMMENTS ──
function PostCard({ post, currentUser }) {
  const { setShowAppRedirect } = useAuth();
  const [timeLeft, setTimeLeft] = useState('');
  const [showComments, setShowComments] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);

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

  useEffect(() => {
    if (!showComments) return;

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

    fetchComments();
  }, [showComments, post.id]);

  const likesCount = post.likeCount || 0;
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
              {post.verified && (
                <svg viewBox="0 0 24 24" fill="#00D9FF" width="14" height="14" style={{ display: 'inline-block', verticalAlign: 'middle', marginLeft: '4px' }}>
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10,-4.48 10,-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                </svg>
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
          onClick={() => setShowAppRedirect('posts')}
        >
          <Heart size={16} fill={post.likedByMe ? 'var(--neon-pink)' : 'none'} stroke={post.likedByMe ? 'var(--neon-pink)' : 'currentColor'} />
          <span style={{ color: post.likedByMe ? 'var(--neon-pink)' : 'inherit' }}>
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

      {/* Comments Dropdown Feed */}
      {showComments && (
        <div className={styles.commentsSection}>
          <div className={styles.mockCommentInputContainer} onClick={() => setShowAppRedirect('posts')}>
            <input 
              type="text" 
              placeholder="Write a comment... (Tell your fantasy, people will DM you!)" 
              className={styles.mockCommentInput}
              readOnly 
            />
            <button type="button" className={styles.mockCommentSendBtn}>
              <Send size={14} />
            </button>
          </div>

          {commentsLoading ? (
            <div className={styles.commentsLoading}>
              <div className={styles.commentsSpinner} />
            </div>
          ) : comments.length === 0 ? (
            <div className={styles.noComments}>No comments yet. Be the first to comment on the mobile app!</div>
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
