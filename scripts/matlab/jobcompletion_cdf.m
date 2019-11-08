%read data into matirx
%http function 
%fid1 = fopen('./j2000u500bjobsmall.txt'); %?????????????
%fid2 = fopen('./j2000u500heujobsmall.txt'); %?????????????
%fid3 = fopen('./j2000u1000bjobsmall.txt');
%fid4 = fopen('./j2000u1000heujobsmall.txt');
%fid5 = fopen('./j2000u50bjobsmall.txt');
%fid6 = fopen('./j2000u50heujobsmall.txt');
%fid7 = fopen('./j2000u2000bjobsmall.txt');
%fid8 = fopen('./j2000u2000heujobsmall.txt');
%fid8 = fopen('./2u100msb.txt');
%fid9 = fopen('./j2000u5000bjobsmall.txt');
%fid7 = fopen('./2u100msheu.txt');
%fid10 = fopen('./j2000u5000heujobsmall.txt');
%fid6 = fopen('./5u100msheu.txt');
%fid3 = fopen('./5u100msb.txt');
%fid4 = fopen('./3u100msheu.txt');
%fid5 = fopen('./3u100msb.txt');
%fid1 = fopen('./03u100msheu.txt'); %?????????????
%fid2 = fopen('./03u100msb.txt'); %?????????????
%fid1 = fopen('./1point5u100msheu.txt'); %?????????????
%fid2 = fopen('./1point5u100msb.txt'); %?????????????
fid1 = fopen('./4u100-150msheu.txt'); %?????????????
fid2 = fopen('./4u100-150msb.txt'); %?????????????
fid3 = fopen('./u6dnspop.txt');
fid4 = fopen('./u6dnssb.txt');
%fid5 = fopen('./u3wsearchpop.txt');
%fid6 = fopen('./u3wsearchsb.txt');
fid5 = fopen('./u3dnspop.txt');
fid6 = fopen('./u3dnssb.txt');

C1 = textscan(fid1, '%f');
C2 = textscan(fid2, '%f');
C3 = textscan(fid3, '%f');
C4 = textscan(fid4, '%f');
C5 = textscan(fid5, '%f');
C6 = textscan(fid6, '%f');
%C7 = textscan(fid7, '%f');
%C8 = textscan(fid8, '%f');
%C9 = textscan(fid9, '%f');
%C10 = textscan(fid10, '%f');
%fcolse(fid); %????????C?????,?????????????????????
%??????????,??????????%f??,???????????????????.?????C??.?????????","????.
%Delimiter???????????????,????????,???????????
data1 = deal(C1{1});
% ??????????????????????,????????
%?????,????.
data2 = deal(C2{1});
data3 = deal(C3{1});
data4 = deal(C4{1});
data5 = deal(C5{1});
data6 = deal(C6{1});
%data7 = deal(C7{1});
%data8 = deal(C8{1});
%data9 = deal(C9{1});
%data10 = deal(C10{1});
%cdf1 = cdfplot(data1);
%hold on
%x=0:max(data1);
%cdf2 = cdfplot(data2);
%hold on
%cdf3 = cdfplot(data3/max(data3));
%hold on
%cdf7 = cdfplot(data7);
%hold on
%cdf8 = cdfplot(data8);
%hold on
%cdf6 = cdfplot(data6);
%cdf8 = cdfplot(data8);
%hold on
%cdf3 = cdfplot(data3);
%cdf1 = cdfplot(data7);
%hold on
%cdf4 = cdfplot(data4);
%hold on
%cdf7 = cdfplot(data2);
%hold on
%cdf5 = cdfplot(data5);
%hold on
%cdf8 = cdfplot(data6);
%hold on
%cdf3 = cdfplot(data5);
%x=0:max(data1);
%cdf7 = cdfplot(data7);
cdf1 = cdfplot(data1);
hold on
cdf2 = cdfplot(data2);

legend('SB','CNS', 'Location','NE')
%set(cdf1,'color','r','linewidth',1)
%set(cdf2,'color','g','linewidth',1)
set(gcf,'paperpositionmode','auto');
%set(cdf3,'color','b','linewidth',1)
%set(cdf4,'color','c','linewidth',1)
%set(cdf5,'color','m','linewidth',1)
%set(cdf6,'color','y','linewidth',1)
xlabel('Job Latency (sec)');

